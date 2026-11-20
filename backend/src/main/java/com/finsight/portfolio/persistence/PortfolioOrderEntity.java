package com.finsight.portfolio.persistence;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "portfolio_orders")
public class PortfolioOrderEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false, length = 4)
    private String side;

    @Column(name = "order_type", nullable = false, length = 8)
    private String orderType;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(name = "limit_price", precision = 19, scale = 4)
    private BigDecimal limitPrice;

    @Column(name = "executed_price", precision = 19, scale = 4)
    private BigDecimal executedPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal fees;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "filled_at")
    private Instant filledAt;

    protected PortfolioOrderEntity() { }

    public PortfolioOrderEntity(String id, PortfolioEntity portfolio, CompanyEntity company, String side,
            String orderType, BigDecimal quantity, BigDecimal limitPrice, BigDecimal fees, Instant createdAt) {
        this.id = id;
        this.portfolio = portfolio;
        this.company = company;
        this.side = side;
        this.orderType = orderType;
        this.status = "PENDING";
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.fees = fees;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() { return id; }
    public PortfolioEntity getPortfolio() { return portfolio; }
    public CompanyEntity getCompany() { return company; }
    public String getSide() { return side; }
    public String getOrderType() { return orderType; }
    public String getStatus() { return status; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public BigDecimal getFees() { return fees; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getFilledAt() { return filledAt; }

    public void updatePending(CompanyEntity company, String side, String orderType, BigDecimal quantity,
            BigDecimal limitPrice, Instant at) {
        if (!"PENDING".equals(status)) throw new IllegalStateException("Only pending orders can be modified");
        this.company = company;
        this.side = side;
        this.orderType = orderType;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.updatedAt = at;
        this.rejectionReason = null;
    }

    public void fill(BigDecimal price, Instant at) {
        status = "FILLED";
        executedPrice = price;
        filledAt = at;
        updatedAt = at;
        rejectionReason = null;
    }

    public void reject(String reason, Instant at) {
        status = "REJECTED";
        rejectionReason = reason == null ? "Order could not be executed" : reason.substring(0, Math.min(500, reason.length()));
        updatedAt = at;
    }

    public void cancel(Instant at) {
        status = "CANCELLED";
        updatedAt = at;
    }
}
