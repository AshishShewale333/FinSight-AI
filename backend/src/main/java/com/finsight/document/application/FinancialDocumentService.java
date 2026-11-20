package com.finsight.document.application;

import com.finsight.common.error.ApiException;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.document.api.DocumentResponse;
import com.finsight.document.persistence.DocumentChunkEntity;
import com.finsight.document.persistence.DocumentChunkRepository;
import com.finsight.document.persistence.FinancialDocumentEntity;
import com.finsight.document.persistence.FinancialDocumentRepository;
import com.finsight.user.persistence.UserEntity;
import com.finsight.user.persistence.UserRepository;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FinancialDocumentService {
    private static final Logger log = LoggerFactory.getLogger(FinancialDocumentService.class);
    private static final int MAX_CHUNK_CHARS = 1800;
    private static final int CHUNK_OVERLAP = 200;
    private static final int MAX_PAGES = 1000;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final FinancialDocumentRepository documents;
    private final DocumentChunkRepository chunks;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final Clock clock;
    private final Path storageRoot;
    private final long maxFileBytes;

    public FinancialDocumentService(FinancialDocumentRepository documents, DocumentChunkRepository chunks,
            UserRepository users, CompanyRepository companies, ObjectProvider<VectorStore> vectorStoreProvider,
            Clock clock, @Value("${finsight.documents.storage-dir}") String storageDir,
            @Value("${finsight.documents.max-file-size}") org.springframework.util.unit.DataSize maxFileSize) {
        this.documents=documents; this.chunks=chunks; this.users=users; this.companies=companies;
        this.vectorStoreProvider=vectorStoreProvider; this.clock=clock;
        this.storageRoot=Path.of(storageDir).toAbsolutePath().normalize();
        this.maxFileBytes=maxFileSize.toBytes();
    }

    @PostConstruct
    void prepareStorage() {
        try { Files.createDirectories(storageRoot); }
        catch (IOException exception) { throw new IllegalStateException("Document storage directory cannot be created", exception); }
    }

    @Transactional
    public DocumentResponse upload(String userId, MultipartFile file, String companyId,
                                   String financialYear, String documentType) {
        if (file == null || file.isEmpty()) throw badRequest("EMPTY_FILE", "Choose a PDF file to upload");
        if (file.getSize() > maxFileBytes) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                "DOCUMENT_TOO_LARGE", "PDF exceeds the configured upload limit");
        byte[] bytes;
        try { bytes=file.getBytes(); }
        catch (IOException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_READ_FAILED", "Uploaded file could not be read"); }
        if (!isPdf(bytes)) throw badRequest("INVALID_PDF", "Only valid PDF files are accepted");
        validateMetadata(companyId, financialYear, documentType);
        String checksum = sha256(bytes);
        if (documents.existsByOwnerIdAndChecksum(userId, checksum)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_DOCUMENT", "This PDF has already been uploaded to your account");
        }
        UserEntity owner = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        CompanyEntity company = companyId == null || companyId.isBlank() ? null : companies.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company was not found"));
        String id = UUID.randomUUID().toString();
        String filename = safeFilename(file.getOriginalFilename());
        String storageKey = id + ".pdf";
        Path target = storageRoot.resolve(storageKey).normalize();
        if (!target.startsWith(storageRoot)) throw new IllegalStateException("Resolved document path escaped storage directory");
        try { Files.write(target, bytes); }
        catch (IOException exception) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_STORE_FAILED", "PDF could not be stored"); }
        Instant now = clock.instant();
        FinancialDocumentEntity entity = documents.save(new FinancialDocumentEntity(id, owner, company,
                emptyToNull(financialYear), documentType == null ? "OTHER" : documentType,
                filename, storageKey, "application/pdf", checksum, now));
        try {
            List<PageChunk> pageChunks = extract(target);
            if (pageChunks.isEmpty()) {
                entity.markFailed("No readable text was extracted from this PDF", clock.instant());
                log.warn("Document processing completed without readable text documentId={}", id);
                return response(entity);
            }
            List<DocumentChunkEntity> savedChunks = new ArrayList<>();
            List<Document> vectors = new ArrayList<>();
            int ordinal = 0;
            for (PageChunk pageChunk : pageChunks) {
                String chunkId = UUID.randomUUID().toString();
                DocumentChunkEntity chunk = new DocumentChunkEntity(chunkId, entity, ordinal++,
                        pageChunk.page(), pageChunk.page(), pageChunk.text(), clock.instant());
                savedChunks.add(chunk);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("owner_id", userId);
                metadata.put("document_id", id);
                metadata.put("chunk_id", chunkId);
                metadata.put("document_type", entityType(documentType));
                metadata.put("page_start", pageChunk.page());
                metadata.put("page_end", pageChunk.page());
                if (company != null) metadata.put("company_id", company.getId());
                if (financialYear != null && !financialYear.isBlank()) metadata.put("financial_year", financialYear);
                vectors.add(new Document(chunkId, pageChunk.text(), metadata));
            }
            chunks.saveAll(savedChunks);
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) {
                entity.markFailed("Embedding and vector search are not configured", clock.instant());
                log.warn("Document indexed metadata but vector store is unavailable documentId={} chunks={}", id, savedChunks.size());
                return response(entity);
            }
            vectorStore.add(vectors);
            savedChunks.forEach(chunk -> chunk.indexed(chunk.getId()));
            chunks.saveAll(savedChunks);
            entity.markReady(countPages(target), clock.instant());
            log.info("Document ready documentId={} pages={} chunks={}", id, countPages(target), savedChunks.size());
        } catch (Exception exception) {
            entity.markFailed("PDF processing or vector indexing failed", clock.instant());
            log.warn("Document processing failed documentId={}", id, exception);
        }
        return response(entity);
    }

    @Transactional(readOnly=true)
    public Page<DocumentResponse> list(String userId, int page, int size) {
        return documents.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(userId,
                FinancialDocumentEntity.Status.DELETED, PageRequest.of(page, size)).map(this::response);
    }

    @Transactional(readOnly=true)
    public DocumentResponse get(String userId, String id) {
        return documents.findByIdAndOwnerId(id, userId).filter(document -> document.getStatus() != FinancialDocumentEntity.Status.DELETED)
                .map(this::response).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document was not found"));
    }

    @Transactional
    public void delete(String userId, String id) {
        FinancialDocumentEntity entity = documents.findByIdAndOwnerId(id, userId)
                .filter(document -> document.getStatus() != FinancialDocumentEntity.Status.DELETED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document was not found"));
        List<String> vectorIds = chunks.findByDocumentIdOrderByOrdinalAsc(id).stream()
                .map(DocumentChunkEntity::getQdrantPointId).filter(java.util.Objects::nonNull).toList();
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null && !vectorIds.isEmpty()) {
            try { vectorStore.delete(vectorIds); }
            catch (Exception exception) { log.warn("Vector cleanup failed documentId={}", id, exception); }
        }
        try { Files.deleteIfExists(resolveStorageKey(entity.getStorageKey())); }
        catch (IOException exception) { log.warn("Document file cleanup failed documentId={}", id, exception); }
        entity.markDeleted(clock.instant());
        log.info("Document deleted documentId={}", id);
    }

    @Transactional(readOnly=true)
    public List<RetrievedChunk> retrieve(String ownerId, String question, String documentId,
                                         String companyId, String financialYear, String documentType, int topK) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) return List.of();
        StringBuilder filter = new StringBuilder("owner_id == '").append(ownerId).append("'");
        if (documentId != null) {
            verifyOwnedReadyDocument(ownerId, documentId);
            filter.append(" && document_id == '").append(documentId).append("'");
        }
        if (companyId != null) filter.append(" && company_id == '").append(companyId).append("'");
        if (financialYear != null) filter.append(" && financial_year == '").append(escapeFilterValue(financialYear)).append("'");
        if (documentType != null) filter.append(" && document_type == '").append(escapeFilterValue(documentType)).append("'");
        List<Document> found = vectorStore.similaritySearch(SearchRequest.builder().query(question)
                .topK(Math.min(Math.max(topK, 1), 10)).similarityThreshold(0.45)
                .filterExpression(filter.toString()).build());
        List<String> ids = found.stream().map(doc -> doc.getMetadata().getOrDefault("chunk_id", doc.getId()).toString()).toList();
        Map<String, DocumentChunkEntity> allowed = new HashMap<>();
        chunks.findByIdInAndDocumentOwnerId(ids, ownerId).stream()
                .filter(chunk -> chunk.getDocument().getStatus() == FinancialDocumentEntity.Status.READY)
                .forEach(chunk -> allowed.put(chunk.getId(), chunk));
        return ids.stream().distinct().map(allowed::get).filter(java.util.Objects::nonNull)
                .map(chunk -> new RetrievedChunk(chunk.getId(), chunk.getDocument().getId(),
                        chunk.getDocument().getOriginalFilename(), chunk.getPageStart(), chunk.getPageEnd(), chunk.getContent())).toList();
    }

    private void verifyOwnedReadyDocument(String ownerId, String documentId) {
        documents.findByIdAndOwnerId(documentId, ownerId)
                .filter(doc -> doc.getStatus() == FinancialDocumentEntity.Status.READY)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document was not found"));
    }

    private List<PageChunk> extract(Path file) throws IOException {
        List<PageChunk> result = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
            if (pdf.isEncrypted()) throw new IOException("Encrypted PDFs are not supported");
            int pages = pdf.getNumberOfPages();
            if (pages < 1 || pages > MAX_PAGES) throw new IOException("PDF page count is outside the supported range");
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf).replaceAll("\\s+", " ").trim();
                for (String chunk : chunkPage(text)) result.add(new PageChunk(page, chunk));
            }
        }
        return result;
    }

    private List<String> chunkPage(String text) {
        if (text.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_CHARS, text.length());
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > start + MAX_CHUNK_CHARS / 2) end = boundary;
            }
            String value = text.substring(start, end).trim();
            if (!value.isBlank()) result.add(value);
            if (end >= text.length()) break;
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return result;
    }

    private int countPages(Path file) throws IOException { try (PDDocument pdf=Loader.loadPDF(file.toFile())) { return pdf.getNumberOfPages(); } }
    private boolean isPdf(byte[] bytes) {
        if (bytes.length < PDF_SIGNATURE.length) return false;
        for (int i=0; i<PDF_SIGNATURE.length; i++) if (bytes[i] != PDF_SIGNATURE[i]) return false;
        return true;
    }
    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "financial-report.pdf";
        String normalized = filename.replace('\\', '/');
        String leaf = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_");
        return leaf.length() <= 255 ? leaf : leaf.substring(0, 255);
    }
    private Path resolveStorageKey(String key) {
        Path path=storageRoot.resolve(key).normalize();
        if (!path.startsWith(storageRoot)) throw new IllegalStateException("Invalid document storage key");
        return path;
    }
    private void validateMetadata(String companyId, String year, String type) {
        if (companyId != null && !companyId.isBlank() && !companyId.matches("[A-Fa-f0-9-]{36}")) throw badRequest("INVALID_COMPANY_ID", "Company ID is invalid");
        if (year != null && !year.isBlank() && !year.matches("(?:[0-9]{4}(?:-[0-9]{2})?|FY[0-9]{4}-[0-9]{2})")) throw badRequest("INVALID_FINANCIAL_YEAR", "Financial year is invalid");
        if (type != null && !type.isBlank() && !List.of("ANNUAL_REPORT", "QUARTERLY_REPORT", "EARNINGS_RELEASE", "OTHER").contains(type)) throw badRequest("INVALID_DOCUMENT_TYPE", "Document type is invalid");
    }
    private String entityType(String type) { return type == null || type.isBlank() ? "OTHER" : type; }
    private String escapeFilterValue(String value) { return value.replace("\\", "\\\\").replace("'", "\\'"); }
    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private DocumentResponse response(FinancialDocumentEntity entity) {
        return new DocumentResponse(entity.getId(), entity.getCompany() == null ? null : entity.getCompany().getId(),
                entity.getFinancialYear(), entity.getDocumentType(), entity.getOriginalFilename(),
                entity.getStatus().name(), entity.getPageCount(), entity.getFailureReason(), entity.getCreatedAt());
    }
    private ApiException badRequest(String code, String message) { return new ApiException(HttpStatus.BAD_REQUEST, code, message); }

    public record RetrievedChunk(String id, String documentId, String filename, int pageStart,
                                 int pageEnd, String content) { }
    private record PageChunk(int page, String text) { }
}
