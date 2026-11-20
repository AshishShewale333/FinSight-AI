package com.finsight.analytics.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;

public final class XirrCalculator {
    private static final MathContext MC = MathContext.DECIMAL128;
    private XirrCalculator() { }

    public static OptionalDouble calculate(List<CashFlow> cashFlows) {
        if (cashFlows == null || cashFlows.size() < 2) return OptionalDouble.empty();
        Instant origin = cashFlows.stream().map(CashFlow::date).min(Instant::compareTo).orElseThrow();
        boolean hasNegative = cashFlows.stream().anyMatch(flow -> flow.amount().signum() < 0);
        boolean hasPositive = cashFlows.stream().anyMatch(flow -> flow.amount().signum() > 0);
        if (!hasNegative || !hasPositive) return OptionalDouble.empty();
        double low = -0.999999;
        double high = 100.0;
        double lowNpv = npv(cashFlows, origin, low);
        double highNpv = npv(cashFlows, origin, high);
        if (!Double.isFinite(lowNpv) || !Double.isFinite(highNpv) || Math.signum(lowNpv) == Math.signum(highNpv)) {
            return OptionalDouble.empty();
        }
        for (int i = 0; i < 256; i++) {
            double mid = (low + high) / 2.0;
            double value = npv(cashFlows, origin, mid);
            if (!Double.isFinite(value)) return OptionalDouble.empty();
            if (Math.abs(value) < 0.000001 || Math.abs(high - low) < 1e-10) return OptionalDouble.of(mid);
            if (Math.signum(value) == Math.signum(lowNpv)) {
                low = mid;
                lowNpv = value;
            } else {
                high = mid;
            }
        }
        return OptionalDouble.of((low + high) / 2.0);
    }

    private static double npv(List<CashFlow> flows, Instant origin, double rate) {
        double value = 0;
        for (CashFlow flow : flows) {
            double years = Duration.between(origin, flow.date()).toSeconds() / (365.25 * 24 * 60 * 60);
            value += flow.amount().doubleValue() / Math.pow(1 + rate, years);
        }
        return value;
    }
}
