package com.finsight.portfolio.persistence;

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
@Table(name = "portfolios")
public class PortfolioEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length = 36, nullable = false)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;
    @Column(nullable = false)
    private boolean archived;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PortfolioEntity() { }
    public PortfolioEntity(String id, UserEntity user, String name, String baseCurrency, Instant now) {
        this.id=id; this.user=user; this.name=name; this.baseCurrency=baseCurrency;
        this.createdAt=now; this.updatedAt=now; this.archived=false;
    }
    public String getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getName() { return name; }
    public String getBaseCurrency() { return baseCurrency; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void archive(Instant now) { archived=true; updatedAt=now; }
    public void touch(Instant now) { updatedAt=now; }
}



