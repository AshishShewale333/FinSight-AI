package com.finsight.analytics.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalDouble;

public final class RiskMetricsCalculator {
    private RiskMetricsCalculator() { }

    public static OptionalDouble annualizedVolatility(List<BigDecimal> closes) {
        if (closes == null || closes.size() < 3) return OptionalDouble.empty();
        double[] returns = new double[closes.size() - 1];
        for (int i = 1; i < closes.size(); i++) {
            if (closes.get(i - 1).signum() <= 0) return OptionalDouble.empty();
            returns[i - 1] = closes.get(i).divide(closes.get(i - 1), 12, java.math.RoundingMode.HALF_UP)
                    .doubleValue() - 1.0;
        }
        double mean = java.util.Arrays.stream(returns).average().orElse(0);
        double variance = java.util.Arrays.stream(returns).map(value -> (value - mean) * (value - mean)).sum()
                / (returns.length - 1);
        return OptionalDouble.of(Math.sqrt(variance * 252.0));
    }

    public static OptionalDouble maximumDrawdown(List<BigDecimal> values) {
        if (values == null || values.size() < 2) return OptionalDouble.empty();
        BigDecimal peak = values.get(0);
        double max = 0;
        for (BigDecimal value : values) {
            if (value.compareTo(peak) > 0) peak = value;
            if (peak.signum() > 0) {
                double drawdown = value.subtract(peak).divide(peak, 12, java.math.RoundingMode.HALF_UP).doubleValue();
                max = Math.min(max, drawdown);
            }
        }
        return OptionalDouble.of(max);
    }
}
