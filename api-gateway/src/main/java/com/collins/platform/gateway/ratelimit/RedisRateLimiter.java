package com.collins.platform.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

/**
 * Redis-backed rate limiter using token bucket algorithm.
 * Thread-safe and works across multiple gateway instances.
 * Uses Lua script for atomic operations.
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final int requestsPerSecond;
    private final RedisScript<List> rateLimitScript;

    private static final String KEY_PREFIX = "rate_limit:";

    /**
     * Lua script for atomic token bucket rate limiting in Redis.
     * Returns: {allowed (0/1), remaining_tokens, reset_time, retry_after}
     */
    private static final String RATE_LIMIT_LUA_SCRIPT = """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local rate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])

        local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
        local tokens = tonumber(bucket[1])
        local last_refill = tonumber(bucket[2])

        -- Initialize if bucket doesn't exist
        if tokens == nil then
          tokens = capacity
          last_refill = now
        end

        -- Refill tokens based on time elapsed
        local elapsed = now - last_refill
        local refill = math.floor(elapsed * rate / 1000)
        tokens = math.min(capacity, tokens + refill)
        last_refill = now

        local allowed = 0
        local retry_after = 0

        if tokens >= requested then
          tokens = tokens - requested
          allowed = 1
        else
          -- Calculate retry-after time in seconds
          local tokens_needed = requested - tokens
          retry_after = math.ceil(tokens_needed / rate) + 1
        end

        -- Save updated bucket state with TTL
        redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
        redis.call('EXPIRE', key, 3600)  -- 1 hour TTL for cleanup

        return {allowed, math.floor(tokens / 1000), math.floor(now / 1000), retry_after}
        """;

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate, int requestsPerSecond) {
        this.redisTemplate = redisTemplate;
        this.requestsPerSecond = requestsPerSecond;
        this.rateLimitScript = RedisScript.of(RATE_LIMIT_LUA_SCRIPT, List.class);
        log.info("RedisRateLimiter initialized: requestsPerSecond={}", requestsPerSecond);
    }

    @Override
    public RateLimitResult checkRateLimit(String clientKey) {
        String redisKey = KEY_PREFIX + clientKey;
        long now = System.currentTimeMillis();
        long capacity = requestsPerSecond * 1000L; // milliTokens
        long requested = 1000L; // Request 1 token (1000 milliTokens)

        try {
            List<Long> result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(redisKey),
                String.valueOf(capacity),
                String.valueOf(requestsPerSecond),
                String.valueOf(now),
                String.valueOf(requested)
            );

            if (result != null && result.size() == 4) {
                boolean allowed = result.get(0) == 1;
                long remainingTokens = result.get(1);
                long resetTimeSeconds = result.get(2);
                long retryAfterSeconds = result.get(3);

                log.debug("Rate limit check for {}: allowed={}, remaining={}",
                         clientKey, allowed, remainingTokens);

                return new RateLimitResult(allowed, remainingTokens, resetTimeSeconds, retryAfterSeconds);
            } else {
                log.error("Unexpected Redis script result: {}", result);
                // Fail open: allow the request if Redis fails
                return new RateLimitResult(true, requestsPerSecond, now / 1000, 0);
            }
        } catch (Exception e) {
            log.error("Redis rate limit check failed for {}, failing open", clientKey, e);
            // Fail open: allow the request if Redis is unavailable
            return new RateLimitResult(true, requestsPerSecond, now / 1000, 0);
        }
    }
}