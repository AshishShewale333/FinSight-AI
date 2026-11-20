package com.finsight.notification.application;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceAlertTriggeredEvent(String alertId, int triggerCount, String recipient,
        String symbol, String comparison, BigDecimal thresholdPrice, BigDecimal currentPrice,
        String currency, Instant triggeredAt) { }
