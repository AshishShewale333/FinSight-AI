package com.finsight.company.metrics;

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
import java.time.LocalDate;


@Entity
@Table(name="company_financial_metrics")
public class CompanyFinancialMetricsEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36, nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="company_id", nullable=false) private CompanyEntity company;
    @Column(name="fiscal_period", nullable=false, length=32) private String fiscalPeriod;
    @Column(name="period_end", nullable=false) private LocalDate periodEnd;
    @Column(precision=24, scale=2) private BigDecimal revenue;
    @Column(name="operating_profit", precision=24, scale=2) private BigDecimal operatingProfit;
    @Column(name="net_profit", precision=24, scale=2) private BigDecimal netProfit;
    @Column(name="total_debt", precision=24, scale=2) private BigDecimal totalDebt;
    @Column(name="total_equity", precision=24, scale=2) private BigDecimal totalEquity;
    @Column(name="total_assets", precision=24, scale=2) private BigDecimal totalAssets;
    @Column(nullable=false, length=3) private String currency;
    @Column(nullable=false, length=100) private String source;
    @Column(name="observed_at", nullable=false) private Instant observedAt;
    protected CompanyFinancialMetricsEntity() { }
    public String getId(){return id;} public CompanyEntity getCompany(){return company;}
    public String getFiscalPeriod(){return fiscalPeriod;} public LocalDate getPeriodEnd(){return periodEnd;}
    public BigDecimal getRevenue(){return revenue;} public BigDecimal getOperatingProfit(){return operatingProfit;}
    public BigDecimal getNetProfit(){return netProfit;} public BigDecimal getTotalDebt(){return totalDebt;}
    public BigDecimal getTotalEquity(){return totalEquity;} public BigDecimal getTotalAssets(){return totalAssets;}
    public String getCurrency(){return currency;} public String getSource(){return source;} public Instant getObservedAt(){return observedAt;}
}



