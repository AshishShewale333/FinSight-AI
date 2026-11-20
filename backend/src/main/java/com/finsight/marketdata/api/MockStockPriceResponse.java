package com.finsight.marketdata.api;

import java.math.BigDecimal;
import java.time.Instant;

public record MockStockPriceResponse(String symbol, BigDecimal price, String currency,
                                     Instant observedAt, String provider, String status) { }
