package com.finsight.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;

@Configuration
@EnableCaching
public class CacheConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    /** Redis is an optimization; database backed screens should still work when it is offline. */
    @Bean
    CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                handle(exception, cache, "read");
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                handle(exception, cache, "write");
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                handle(exception, cache, "evict");
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                handle(exception, cache, "clear");
            }

            private void handle(RuntimeException exception, Cache cache, String operation) {
                if (exception instanceof RedisConnectionFailureException || exception instanceof DataAccessException) {
                    log.warn("Redis cache {} failed for {}; continuing without the cache", operation,
                            cache.getName());
                    return;
                }
                throw exception;
            }
        };
    }
}
