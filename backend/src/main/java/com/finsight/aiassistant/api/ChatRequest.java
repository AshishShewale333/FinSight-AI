package com.finsight.aiassistant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(@NotBlank @Pattern(regexp="^[A-Fa-f0-9-]{36}$") String sessionId,
                          @NotBlank @Size(max=4000) String question) { }
