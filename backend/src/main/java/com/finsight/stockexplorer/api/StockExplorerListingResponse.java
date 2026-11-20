package com.finsight.stockexplorer.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockExplorerListingResponse(String id, String exchange, String symbol, String name,
        String series, LocalDate listingDate, BigDecimal paidUpValue, Integer marketLot,
        String isin, BigDecimal faceValue, boolean active) implements java.io.Serializable { }
