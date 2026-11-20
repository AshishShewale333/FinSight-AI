package com.finsight.analytics.api;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioAnalyticsResponse(
        BigDecimal totalInvestment,
        BigDecimal currentPortfolioValue,
        BigDecimal realizedProfitLoss,
        BigDecimal unrealizedProfitLoss,
        BigDecimal absoluteReturns,
        BigDecimal percentageReturns,
        Double xirr,
        Double cagr,
        Map<String, BigDecimal> assetAllocation,
        BigDecimal concentrationHhi,
        Double annualizedVolatility,
        Double maximumDrawdown,
        boolean valuationComplete,
        int unpricedHoldingCount,
        String calculationNotes) { }
