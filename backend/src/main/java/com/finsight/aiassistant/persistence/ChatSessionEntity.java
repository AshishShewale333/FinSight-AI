package com.finsight.aiassistant.persistence;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.document.persistence.FinancialDocumentEntity;
import com.finsight.user.persistence.UserEntity;

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
@Table(name="chat_sessions")
public class ChatSessionEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36, nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private UserEntity user;
    @Column(nullable=false, length=200) private String title;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") private CompanyEntity company;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id") private FinancialDocumentEntity document;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    protected ChatSessionEntity() { }
    public ChatSessionEntity(String id, UserEntity user, String title, CompanyEntity company,
                             FinancialDocumentEntity document, Instant now) {
        this.id=id; this.user=user; this.title=title; this.company=company; this.document=document;
        this.createdAt=now; this.updatedAt=now;
    }
    public String getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getTitle() { return title; }
    public CompanyEntity getCompany() { return company; }
    public FinancialDocumentEntity getDocument() { return document; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touched(Instant now) { updatedAt=now; }
}


