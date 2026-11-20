package com.finsight.marketdata.application;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class MarketPriceRegistry {
    private final ConcurrentMap<String, PriceSnapshot> latestByCompany = new ConcurrentHashMap<>();

    public void update(String companyId, String symbol, BigDecimal price, String currency, Instant observedAt) {
        latestByCompany.put(companyId, new PriceSnapshot(companyId, symbol, price, currency, observedAt));
    }

    public Optional<PriceSnapshot> latest(String companyId) {
        return Optional.ofNullable(latestByCompany.get(companyId));
    }

    public record PriceSnapshot(String companyId, String symbol, BigDecimal price, String currency,
                                Instant observedAt) { }
}
