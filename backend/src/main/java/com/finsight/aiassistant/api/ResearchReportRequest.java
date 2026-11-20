package com.finsight.aiassistant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResearchReportRequest(@NotBlank @Pattern(regexp="^[A-Fa-f0-9-]{36}$") String companyId) { }
