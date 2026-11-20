package com.finsight.aiassistant.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ChatAnswerResponse(String sessionId, String answer,
                                 List<Map<String,Object>> citations,
                                 List<String> interpretations,
                                 List<String> missingInformation,
                                 String disclaimer, String model, Instant createdAt) { }
