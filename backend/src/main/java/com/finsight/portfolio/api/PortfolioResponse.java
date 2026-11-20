package com.finsight.portfolio.api;

import java.time.Instant;

public record PortfolioResponse(String id, String name, String baseCurrency, Instant createdAt, Instant updatedAt) { }
