package com.finsight.document.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FinancialDocumentRepository extends JpaRepository<FinancialDocumentEntity, String> {
    Page<FinancialDocumentEntity> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(String ownerId,
            FinancialDocumentEntity.Status status, Pageable pageable);
    Optional<FinancialDocumentEntity> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByOwnerIdAndChecksum(String ownerId, String checksum);
}
