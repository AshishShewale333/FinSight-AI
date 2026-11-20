package com.finsight.notification.api;

import java.time.Instant;

public record NotificationResponse(String id, String type, String title, String body,
                                   Instant createdAt, Instant readAt) { }
