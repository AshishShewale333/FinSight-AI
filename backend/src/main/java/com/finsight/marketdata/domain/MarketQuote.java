package com.finsight.marketdata.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(String companyId, BigDecimal price, String currency, String provider,
                          QuoteStatus status, Instant observedAt) { }
