package com.finsight.aiassistant.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ChatMessageResponse(String id, long sequence, String role, String content,
                                  List<Map<String,Object>> citations, Instant createdAt) { }
