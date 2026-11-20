package com.finsight.company.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "companies")
public class CompanyEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length = 36, nullable = false)
    private String id;
    @Column(name = "exchange_code", nullable = false, length = 8)
    private String exchangeCode;
    @Column(nullable = false, length = 32)
    private String symbol;
    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;
    @Column(length = 12)
    private String isin;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(length = 120)
    private String sector;
    private String description;
    @Column(nullable = false)
    private boolean active;

    protected CompanyEntity() { }
    public String getId() { return id; }
    public String getExchangeCode() { return exchangeCode; }
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public String getIsin() { return isin; }
    public String getCurrency() { return currency; }
    public String getSector() { return sector; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}



