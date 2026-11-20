package com.finsight.portfolio.api;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(String id, String companyId, String symbol, String side, String orderType,
        String status, BigDecimal quantity, BigDecimal limitPrice, BigDecimal executedPrice,
        BigDecimal fees, String currency, String rejectionReason, Instant createdAt,
        Instant updatedAt, Instant filledAt) { }
