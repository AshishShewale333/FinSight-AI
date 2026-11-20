package com.finsight.authentication.api;

import java.time.Instant;

public record TokenResponse(String tokenType, String accessToken, String refreshToken,
                            long expiresIn, Instant issuedAt) { }
