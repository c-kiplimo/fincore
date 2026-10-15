package com.collins.platform.gateway.ratelimit;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory rate limiter using token bucket algorithm.
 * Suitable for single-instance deployments or development.
 * Not suitable for distributed multi-instance production deployments.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimiter.class);

    private static final long BUCKET_EXPIRY_MINUTES = 60;
    private static final long CLEANUP_INTERVAL_MINUTES = 15;

    // Token bucket: clientKey -> [milliTokens, lastRefillTimestamp]
    private final Map<String, long[]> buckets = new ConcurrentHashMap<>();
    private final int requestsPerSecond;
    private final ScheduledExecutorService cleanupExecutor;

    public InMemoryRateLimiter(int requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "rate-limit-bucket-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        log.info("InMemoryRateLimiter initialized: requestsPerSecond={}", requestsPerSecond);
    }

    @PostConstruct
    public void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredBuckets,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        log.info("Rate limit bucket cleanup task scheduled: interval={}min, expiry={}min",
                 CLEANUP_INTERVAL_MINUTES, BUCKET_EXPIRY_MINUTES);
    }

    private void cleanupExpiredBuckets() {
        try {
            long now = System.currentTimeMillis();
            long expiryThreshold = now - TimeUnit.MINUTES.toMillis(BUCKET_EXPIRY_MINUTES);
            int beforeSize = buckets.size();

            buckets.entrySet().removeIf(entry -> entry.getValue()[1] < expiryThreshold);

            int removed = beforeSize - buckets.size();
            if (removed > 0) {
                log.info("Rate limit bucket cleanup: removed={}, remaining={}", removed, buckets.size());
            }
        } catch (Exception e) {
            log.error("Error during rate limit bucket cleanup", e);
        }
    }

    @Override
    public RateLimitResult checkRateLimit(String clientKey) {
        long now = System.currentTimeMillis();
        long capacity = requestsPerSecond * 1000L;

        long[] bucket = buckets.computeIfAbsent(clientKey, k -> {
            log.debug("Created new rate limit bucket for client: {}", k);
            return new long[]{capacity, now};
        });

        synchronized (bucket) {
            long elapsedMillis = now - bucket[1];
            long tokensToAdd = elapsedMillis * requestsPerSecond;

            bucket[0] = Math.min(capacity, bucket[0] + tokensToAdd);
            bucket[1] = now;

            long remainingTokens = bucket[0] / 1000;

            if (bucket[0] >= 1000) {
                bucket[0] -= 1000;
                return new RateLimitResult(true, remainingTokens, now / 1000, 0);
            } else {
                long tokensNeeded = 1000 - bucket[0];
                long retryAfterSeconds = (tokensNeeded / requestsPerSecond) + 1;
                return new RateLimitResult(false, 0, now / 1000, retryAfterSeconds);
            }
        }
    }
}