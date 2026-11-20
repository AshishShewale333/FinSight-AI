package com.finsight.company.api;

public record CompanyResponse(String id, String exchange, String symbol, String name,
                              String isin, String currency, String sector, boolean active) { }
