package com.finsight.notification.persistence;

import com.finsight.company.persistence.CompanyEntity;
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

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name="price_alerts")
public class PriceAlertEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36, nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private UserEntity user;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="company_id", nullable=false) private CompanyEntity company;
    @Column(nullable=false, length=8) private String comparison;
    @Column(name="threshold_price", nullable=false, precision=19, scale=4) private BigDecimal thresholdPrice;
    @Column(nullable=false, length=3) private String currency;
    @Column(nullable=false) private boolean enabled;
    @Column(name="last_triggered_at") private Instant lastTriggeredAt;
    @Column(name="trigger_count", nullable=false) private int triggerCount;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    protected PriceAlertEntity() { }
    public PriceAlertEntity(String id, UserEntity user, CompanyEntity company, String comparison,
                            BigDecimal thresholdPrice, String currency, Instant createdAt) {
        this.id=id; this.user=user; this.company=company; this.comparison=comparison;
        this.thresholdPrice=thresholdPrice; this.currency=currency; this.createdAt=createdAt; this.enabled=true;
    }
    public String getId() { return id; }
    public UserEntity getUser() { return user; }
    public CompanyEntity getCompany() { return company; }
    public String getComparison() { return comparison; }
    public BigDecimal getThresholdPrice() { return thresholdPrice; }
    public String getCurrency() { return currency; }
    public boolean isEnabled() { return enabled; }
    public Instant getLastTriggeredAt() { return lastTriggeredAt; }
    public int getTriggerCount() { return triggerCount; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean crossed(BigDecimal previousPrice, BigDecimal currentPrice) {
        if (!enabled) return false;
        if ("ABOVE".equals(comparison)) {
            return previousPrice.compareTo(thresholdPrice) <= 0 && currentPrice.compareTo(thresholdPrice) > 0;
        }
        return previousPrice.compareTo(thresholdPrice) >= 0 && currentPrice.compareTo(thresholdPrice) < 0;
    }
    public void trigger(Instant now) { enabled=false; lastTriggeredAt=now; triggerCount++; }
    public void reactivate() { enabled=true; }
}



