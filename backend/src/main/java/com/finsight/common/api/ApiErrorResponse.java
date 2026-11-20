package com.finsight.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(boolean success, String message, String code,
                               Map<String, String> fieldErrors, Instant timestamp,
                               String correlationId) { }
