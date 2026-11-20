package com.finsight.marketdata.api;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponse(String companyId, BigDecimal price, String currency, String provider,
                            String status, Instant observedAt, boolean stale) implements java.io.Serializable { }
