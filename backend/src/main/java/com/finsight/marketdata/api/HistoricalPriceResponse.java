package com.finsight.marketdata.api;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalPriceResponse(Instant timestamp,BigDecimal open,BigDecimal high,BigDecimal low,
                                      BigDecimal close,Long volume,String currency,String provider) { }
