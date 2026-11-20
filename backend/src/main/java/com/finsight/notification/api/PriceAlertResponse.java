package com.finsight.notification.api;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceAlertResponse(String id, String companyId, String symbol, String comparison,
                                 BigDecimal thresholdPrice, String currency, boolean enabled,
                                 Instant lastTriggeredAt, Instant createdAt) { }
