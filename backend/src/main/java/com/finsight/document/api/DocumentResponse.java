package com.finsight.document.api;

import java.time.Instant;

public record DocumentResponse(String id, String companyId, String financialYear, String documentType,
                               String filename, String status, Integer pageCount,
                               String failureReason, Instant createdAt) { }
