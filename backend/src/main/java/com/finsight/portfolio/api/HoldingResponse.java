package com.finsight.portfolio.api;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingResponse(String companyId, String exchange, String symbol, String companyName,
                              String currency, BigDecimal quantity, BigDecimal averageCost, BigDecimal costBasis,
                              BigDecimal currentPrice, BigDecimal marketValue, BigDecimal unrealizedProfitLoss,
                              BigDecimal unrealizedProfitLossPercentage,
                              String quoteStatus, Instant quoteObservedAt) { }
