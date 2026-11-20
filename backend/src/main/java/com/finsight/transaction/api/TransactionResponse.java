package com.finsight.transaction.api;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(String id, String companyId, String companySymbol, String type,
                                  BigDecimal quantity, BigDecimal unitPrice, BigDecimal fees,
                                  String currency, Instant tradedAt, BigDecimal realizedProfitLoss) { }
