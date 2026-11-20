package com.finsight.document.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.document.application.FinancialDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@RequestMapping("/api/v1/documents")
public class FinancialDocumentController {
    private final FinancialDocumentService service;
    public FinancialDocumentController(FinancialDocumentService service) { this.service=service; }

    @PostMapping(consumes="multipart/form-data")
    public ApiResponse<DocumentResponse> upload(@AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required=false) String companyId,
            @RequestParam(required=false) String financialYear,
            @RequestParam(required=false, defaultValue="OTHER") String documentType,
            HttpServletRequest request) {
        return ApiResponse.success("Document processed", service.upload(jwt.getSubject(), file,
                companyId, financialYear, documentType), cid(request));
    }

    @GetMapping
    public ApiResponse<Page<DocumentResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        return ApiResponse.success("Documents retrieved", service.list(jwt.getSubject(), page, size), cid(request));
    }

    @GetMapping("/{documentId}")
    public ApiResponse<DocumentResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String documentId,
                                             HttpServletRequest request) {
        return ApiResponse.success("Document retrieved", service.get(jwt.getSubject(), documentId), cid(request));
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String documentId,
                                    HttpServletRequest request) {
        service.delete(jwt.getSubject(), documentId);
        return ApiResponse.success("Document deleted", null, cid(request));
    }

    private String cid(HttpServletRequest request) { return request.getHeader(CorrelationIdFilter.HEADER_NAME); }
}
