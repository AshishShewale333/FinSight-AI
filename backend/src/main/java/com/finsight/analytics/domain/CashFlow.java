package com.finsight.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CashFlow(Instant date, BigDecimal amount) { }
