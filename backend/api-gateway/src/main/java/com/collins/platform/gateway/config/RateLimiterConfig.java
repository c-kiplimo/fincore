package com.collins.platform.gateway.config;

import com.collins.platform.gateway.ratelimit.InMemoryRateLimiter;
import com.collins.platform.gateway.ratelimit.RateLimiter;
import com.collins.platform.gateway.ratelimit.RedisRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Configuration for rate limiting.
 * Supports both in-memory (single instance) and Redis-backed (distributed) rate limiting.
 *
 * <p>Configuration:
 * - gateway.rate-limit.type=redis (for distributed) or memory (for single instance)
 * - gateway.rate-limit.requests-per-second=50 (default)
 */
@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    @ConditionalOnProperty(name = "gateway.rate-limit.type", havingValue = "redis")
    public RateLimiter redisRateLimiter(
            RedisTemplate<String, String> redisTemplate,
            @Value("${gateway.rate-limit.requests-per-second:50}") int requestsPerSecond) {
        log.info("Configuring Redis-backed rate limiter for distributed deployment");
        return new RedisRateLimiter(redisTemplate, requestsPerSecond);
    }

    @Bean
    @ConditionalOnProperty(name = "gateway.rate-limit.type", havingValue = "memory", matchIfMissing = true)
    public RateLimiter inMemoryRateLimiter(
            @Value("${gateway.rate-limit.requests-per-second:50}") int requestsPerSecond) {
        log.warn("Configuring in-memory rate limiter - NOT suitable for multi-instance production deployments");
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(requestsPerSecond);
        limiter.startCleanupTask();
        return limiter;
    }
}