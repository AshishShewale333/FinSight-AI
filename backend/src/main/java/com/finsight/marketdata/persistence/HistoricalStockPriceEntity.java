package com.finsight.marketdata.persistence;

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
@Table(name = "historical_stock_prices")
public class HistoricalStockPriceEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length = 36, nullable = false)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;
    @Column(name = "interval_code", nullable = false, length = 12)
    private String intervalCode;
    @Column(name = "price_time", nullable = false)
    private Instant priceTime;
    @Column(name = "open_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal open;
    @Column(name = "high_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal high;
    @Column(name = "low_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal low;
    @Column(name = "close_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal close;
    @Column
    private Long volume;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, length = 80)
    private String provider;

    protected HistoricalStockPriceEntity() { }
    public String getId() { return id; }
    public CompanyEntity getCompany() { return company; }
    public String getIntervalCode() { return intervalCode; }
    public Instant getPriceTime() { return priceTime; }
    public BigDecimal getOpen() { return open; }
    public BigDecimal getHigh() { return high; }
    public BigDecimal getLow() { return low; }
    public BigDecimal getClose() { return close; }
    public Long getVolume() { return volume; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
}



