package com.collins.platform.gateway.ratelimit;

/**
 * Rate limiter interface for controlling request rates per client.
 * Implementations can use in-memory storage, Redis, or other backends.
 */
public interface RateLimiter {

    /**
     * Checks if a request from the given client should be allowed.
     *
     * @param clientKey Unique identifier for the client
     * @return RateLimitResult containing decision and metadata
     */
    RateLimitResult checkRateLimit(String clientKey);

    /**
     * Result of a rate limit check.
     */
    class RateLimitResult {
        private final boolean allowed;
        private final long remainingTokens;
        private final long resetTimeSeconds;
        private final long retryAfterSeconds;

        public RateLimitResult(boolean allowed, long remainingTokens, long resetTimeSeconds, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.resetTimeSeconds = resetTimeSeconds;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRemainingTokens() {
            return remainingTokens;
        }

        public long getResetTimeSeconds() {
            return resetTimeSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}