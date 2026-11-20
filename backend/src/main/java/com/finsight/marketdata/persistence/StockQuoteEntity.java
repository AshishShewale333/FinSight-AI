package com.finsight.marketdata.persistence;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.marketdata.domain.QuoteStatus;

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

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "stock_quotes")
public class StockQuoteEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length = 36, nullable = false)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, length = 80)
    private String provider;
    @Enumerated(EnumType.STRING)
    @Column(name = "quote_status", nullable = false, length = 16)
    private QuoteStatus status;
    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    protected StockQuoteEntity() { }
    public StockQuoteEntity(String id, CompanyEntity company, BigDecimal price, String currency,
                            String provider, QuoteStatus status, Instant observedAt) {
        this.id=id; this.company=company; this.price=price; this.currency=currency;
        this.provider=provider; this.status=status; this.observedAt=observedAt;
    }
    public String getId() { return id; }
    public CompanyEntity getCompany() { return company; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
    public QuoteStatus getStatus() { return status; }
    public Instant getObservedAt() { return observedAt; }

    public void update(BigDecimal price, String currency, String provider, QuoteStatus status, Instant observedAt) {
        this.price = price;
        this.currency = currency;
        this.provider = provider;
        this.status = status;
        this.observedAt = observedAt;
    }
}



