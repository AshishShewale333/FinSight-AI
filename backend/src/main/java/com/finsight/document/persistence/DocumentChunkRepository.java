package com.finsight.document.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, String> {
    List<DocumentChunkEntity> findByDocumentIdOrderByOrdinalAsc(String documentId);
    Optional<DocumentChunkEntity> findByIdAndDocumentOwnerId(String id, String ownerId);
    List<DocumentChunkEntity> findByIdInAndDocumentOwnerId(List<String> ids, String ownerId);
}
