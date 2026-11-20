package com.finsight.user.api;

import java.time.Instant;

public record UserResponse(String id, String email, String displayName, Instant createdAt) { }
