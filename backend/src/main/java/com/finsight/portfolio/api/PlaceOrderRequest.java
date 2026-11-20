package com.finsight.portfolio.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank String listingId,
        @NotBlank @Pattern(regexp = "BUY|SELL") String side,
        @NotBlank @Pattern(regexp = "MARKET|LIMIT") String orderType,
        @NotNull @DecimalMin("0.00000001") @Digits(integer = 16, fraction = 8) BigDecimal quantity,
        @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal limitPrice) { }
