package com.finsight.notification.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreatePriceAlertRequest(@NotBlank String companyId,
        @NotBlank @Pattern(regexp="ABOVE|BELOW") String comparison,
        @NotNull @DecimalMin("0.0001") BigDecimal thresholdPrice) { }
