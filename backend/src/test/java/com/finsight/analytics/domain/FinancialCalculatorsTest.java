package com.finsight.analytics.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialCalculatorsTest {
    @Test
    void calculatesAnnualizedReturnForOneYearCashFlows() {
        var flows = List.of(
                new CashFlow(Instant.parse("2024-01-01T00:00:00Z"), new BigDecimal("-100")),
                new CashFlow(Instant.parse("2025-01-01T00:00:00Z"), new BigDecimal("110")));

        double expected = Math.pow(1.10, 365.25 / 366.0) - 1.0;
        assertEquals(expected, XirrCalculator.calculate(flows).orElseThrow(), 0.00000001);
    }

    @Test
    void declinesXirrWhenCashFlowsHaveNoInvestmentOrNoRecovery() {
        assertFalse(XirrCalculator.calculate(List.of(
                new CashFlow(Instant.parse("2024-01-01T00:00:00Z"), new BigDecimal("-100")),
                new CashFlow(Instant.parse("2025-01-01T00:00:00Z"), new BigDecimal("-5")))).isPresent());
    }

    @Test
    void computesSampleAnnualizedVolatilityAndMaximumDrawdown() {
        var prices = List.of(new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("99"));
        assertTrue(RiskMetricsCalculator.annualizedVolatility(prices).orElseThrow() > 0);
        assertEquals(-0.10, RiskMetricsCalculator.maximumDrawdown(prices).orElseThrow(), 0.000001);
    }

    @Test
    void reportsRiskMetricsUnavailableForInsufficientHistory() {
        assertFalse(RiskMetricsCalculator.annualizedVolatility(List.of(new BigDecimal("100"), new BigDecimal("101"))).isPresent());
        assertFalse(RiskMetricsCalculator.maximumDrawdown(List.of(new BigDecimal("100"))).isPresent());
    }
}
