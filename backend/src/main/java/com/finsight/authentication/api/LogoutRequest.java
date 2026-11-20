package com.finsight.authentication.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(@NotBlank @Size(min = 32, max = 256) String refreshToken) { }
