package com.finsight.document.persistence;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.user.persistence.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;


@Entity
@Table(name="financial_documents")
public class FinancialDocumentEntity {
    public enum Status { UPLOADED, PROCESSING, READY, FAILED, DELETED }
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36, nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="owner_id", nullable=false) private UserEntity owner;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") private CompanyEntity company;
    @Column(name="financial_year", length=16) private String financialYear;
    @Column(name="document_type", nullable=false, length=40) private String documentType;
    @Column(name="original_filename", nullable=false, length=255) private String originalFilename;
    @Column(name="storage_key", nullable=false, length=500) private String storageKey;
    @Column(name="mime_type", nullable=false, length=100) private String mimeType;
    @Column(nullable=false, length=64) private String checksum;
    @Enumerated(EnumType.STRING) @Column(name="processing_status", nullable=false, length=20) private Status status;
    @Column(name="page_count") private Integer pageCount;
    @Column(name="failure_reason", length=500) private String failureReason;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;

    protected FinancialDocumentEntity() { }
    public FinancialDocumentEntity(String id, UserEntity owner, CompanyEntity company, String financialYear,
            String documentType, String originalFilename, String storageKey, String mimeType,
            String checksum, Instant now) {
        this.id=id; this.owner=owner; this.company=company; this.financialYear=financialYear;
        this.documentType=documentType; this.originalFilename=originalFilename; this.storageKey=storageKey;
        this.mimeType=mimeType; this.checksum=checksum; this.status=Status.PROCESSING;
        this.createdAt=now; this.updatedAt=now;
    }
    public String getId() { return id; }
    public UserEntity getOwner() { return owner; }
    public CompanyEntity getCompany() { return company; }
    public String getFinancialYear() { return financialYear; }
    public String getDocumentType() { return documentType; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageKey() { return storageKey; }
    public Status getStatus() { return status; }
    public Integer getPageCount() { return pageCount; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void markReady(int pages, Instant now) { status=Status.READY; pageCount=pages; failureReason=null; updatedAt=now; }
    public void markFailed(String reason, Instant now) { status=Status.FAILED; failureReason=reason; updatedAt=now; }
    public void markDeleted(Instant now) { status=Status.DELETED; updatedAt=now; }
}



