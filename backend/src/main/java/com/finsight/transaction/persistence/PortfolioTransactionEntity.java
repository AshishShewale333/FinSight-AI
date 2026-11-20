package com.finsight.transaction.persistence;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.portfolio.persistence.PortfolioEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "portfolio_transactions")
public class PortfolioTransactionEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length = 36, nullable = false)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;
    @Column(name = "transaction_type", nullable = false, length = 16)
    private String transactionType;
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal fees;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "traded_at", nullable = false)
    private Instant tradedAt;
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
    @Column(name = "realized_pl", nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedPl;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PortfolioTransactionEntity() { }
    public PortfolioTransactionEntity(String id, PortfolioEntity portfolio, CompanyEntity company,
            String transactionType, BigDecimal quantity, BigDecimal unitPrice, BigDecimal fees,
            String currency, Instant tradedAt, String idempotencyKey, BigDecimal realizedPl, Instant createdAt) {
        this.id=id; this.portfolio=portfolio; this.company=company; this.transactionType=transactionType;
        this.quantity=quantity; this.unitPrice=unitPrice; this.fees=fees; this.currency=currency;
        this.tradedAt=tradedAt; this.idempotencyKey=idempotencyKey; this.realizedPl=realizedPl; this.createdAt=createdAt;
    }
    public String getId() { return id; }
    public PortfolioEntity getPortfolio() { return portfolio; }
    public CompanyEntity getCompany() { return company; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getFees() { return fees; }
    public String getCurrency() { return currency; }
    public Instant getTradedAt() { return tradedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getRealizedPl() { return realizedPl; }
    public Instant getCreatedAt() { return createdAt; }
}



