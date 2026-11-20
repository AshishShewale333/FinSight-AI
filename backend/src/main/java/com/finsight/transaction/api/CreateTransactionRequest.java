package com.finsight.transaction.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransactionRequest(
        @NotBlank String companyId,
        @NotBlank @Pattern(regexp = "BUY|SELL") String type,
        @NotNull @DecimalMin(value = "0.00000001") @Digits(integer = 16, fraction = 8) BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal unitPrice,
        @NotNull @DecimalMin(value = "0.0") @Digits(integer = 15, fraction = 4) BigDecimal fees,
        @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull Instant tradedAt) { }
