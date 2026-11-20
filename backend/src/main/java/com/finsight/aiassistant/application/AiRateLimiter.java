package com.finsight.aiassistant.application;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.finsight.common.error.ApiException;
import java.time.Duration;

@Component
public class AiRateLimiter {
    private final StringRedisTemplate redis;
    private final int limit;
    private final Duration window;

    public AiRateLimiter(StringRedisTemplate redis,
            @org.springframework.beans.factory.annotation.Value("${finsight.ai.rate-limit-per-minute:20}") int limit) {
        this.redis=redis; this.limit=limit; this.window=Duration.ofMinutes(1);
    }

    public void check(String userId) {
        try {
            String key="finsight:ai:rate:" + userId;
            Long count=redis.opsForValue().increment(key);
            if (count != null && count == 1) redis.expire(key, window);
            if (count != null && count > limit) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "AI_RATE_LIMITED", "AI request limit exceeded; try again in a minute");
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                    "AI rate limiting service is temporarily unavailable");
        }
    }
}
