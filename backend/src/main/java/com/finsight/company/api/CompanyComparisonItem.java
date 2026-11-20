package com.finsight.company.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CompanyComparisonItem(String companyId, String exchange, String symbol, String name,
        String currency, String dataStatus, String source, String fiscalPeriod, LocalDate periodEnd,
        BigDecimal revenue, BigDecimal operatingProfit, BigDecimal netProfit,
        BigDecimal debtToEquity, BigDecimal returnOnEquityPercent, Instant observedAt) { }
