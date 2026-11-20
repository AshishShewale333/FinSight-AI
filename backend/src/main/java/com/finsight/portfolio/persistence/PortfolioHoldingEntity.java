package com.finsight.portfolio.persistence;

import com.finsight.company.persistence.CompanyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "portfolio_holdings", uniqueConstraints = @UniqueConstraint(name = "uq_holding_portfolio_company", columnNames = {"portfolio_id", "company_id"}))
public class PortfolioHoldingEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length = 36, nullable = false)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;
    @Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost;
    @Column(name = "cost_basis", nullable = false, precision = 19, scale = 4)
    private BigDecimal costBasis;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PortfolioHoldingEntity() { }
    public PortfolioHoldingEntity(String id, PortfolioEntity portfolio, CompanyEntity company,
                                  BigDecimal quantity, BigDecimal averageCost, BigDecimal costBasis, Instant now) {
        this.id=id; this.portfolio=portfolio; this.company=company; this.quantity=quantity;
        this.averageCost=averageCost; this.costBasis=costBasis; this.updatedAt=now;
    }
    public String getId() { return id; }
    public PortfolioEntity getPortfolio() { return portfolio; }
    public CompanyEntity getCompany() { return company; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAverageCost() { return averageCost; }
    public BigDecimal getCostBasis() { return costBasis; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void buy(BigDecimal addedQuantity, BigDecimal addedCost, Instant now) {
        this.quantity = quantity.add(addedQuantity);
        this.costBasis = costBasis.add(addedCost).setScale(4, java.math.RoundingMode.HALF_UP);
        this.averageCost = this.costBasis.divide(this.quantity, 4, java.math.RoundingMode.HALF_UP);
        this.updatedAt = now;
    }
    public BigDecimal sell(BigDecimal soldQuantity, Instant now) {
        if (soldQuantity.compareTo(quantity) > 0) throw new IllegalArgumentException("Cannot sell more shares than held");
        BigDecimal removedCost = averageCost.multiply(soldQuantity).setScale(4, java.math.RoundingMode.HALF_UP);
        quantity = quantity.subtract(soldQuantity);
        costBasis = costBasis.subtract(removedCost).max(BigDecimal.ZERO).setScale(4, java.math.RoundingMode.HALF_UP);
        averageCost = quantity.signum() == 0 ? BigDecimal.ZERO : costBasis.divide(quantity, 4, java.math.RoundingMode.HALF_UP);
        updatedAt = now;
        return removedCost;
    }
}


