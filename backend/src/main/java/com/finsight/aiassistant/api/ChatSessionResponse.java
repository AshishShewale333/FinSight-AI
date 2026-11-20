package com.finsight.aiassistant.api;

import java.time.Instant;

public record ChatSessionResponse(String id, String title, String companyId, String documentId,
                                 Instant createdAt, Instant updatedAt) { }
