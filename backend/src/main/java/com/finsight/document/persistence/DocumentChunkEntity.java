package com.finsight.document.persistence;

import com.finsight.company.persistence.CompanyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;


@Entity
@Table(name="document_chunks")
public class DocumentChunkEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36, nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="document_id", nullable=false) private FinancialDocumentEntity document;
    @Column(name="chunk_ordinal", nullable=false) private int ordinal;
    @Column(name="page_start", nullable=false) private int pageStart;
    @Column(name="page_end", nullable=false) private int pageEnd;
    @Column(nullable=false, columnDefinition="LONGTEXT") private String content;
    @Column(name="qdrant_point_id", length=64) private String qdrantPointId;
    @Column(name="embedding_status", nullable=false, length=16) private String embeddingStatus;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    protected DocumentChunkEntity() { }
    public DocumentChunkEntity(String id, FinancialDocumentEntity document, int ordinal, int pageStart,
                               int pageEnd, String content, Instant now) {
        this.id=id; this.document=document; this.ordinal=ordinal; this.pageStart=pageStart;
        this.pageEnd=pageEnd; this.content=content; this.embeddingStatus="PENDING"; this.createdAt=now;
    }
    public String getId() { return id; }
    public FinancialDocumentEntity getDocument() { return document; }
    public int getOrdinal() { return ordinal; }
    public int getPageStart() { return pageStart; }
    public int getPageEnd() { return pageEnd; }
    public String getContent() { return content; }
    public String getQdrantPointId() { return qdrantPointId; }
    public void indexed(String pointId) { qdrantPointId=pointId; embeddingStatus="INDEXED"; }
}


