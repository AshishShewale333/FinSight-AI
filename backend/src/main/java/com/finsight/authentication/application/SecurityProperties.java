package com.finsight.authentication.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "finsight.security")
public record SecurityProperties(String jwtSecret, String jwtIssuer,
                                 Duration accessTokenTtl, Duration refreshTokenTtl) { }
