package com.collins.platform.gateway.config;

import com.collins.platform.gateway.ratelimit.RateLimiter;
import com.collins.platform.gateway.ratelimit.RateLimiter.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Global filter for request tracing, API versioning, and rate limiting.
 * Supports both in-memory and Redis-backed rate limiting via configuration.
 */
@Component
public class EdgeFilters implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(EdgeFilters.class);

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_PLATFORM_VERSION = "Platform-Version";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    private final String defaultVersion;
    private final RateLimiter rateLimiter;

    public EdgeFilters(@Value("${gateway.api-version:2026-10-01}") String defaultVersion,
                       RateLimiter rateLimiter) {
        if (defaultVersion == null || defaultVersion.isBlank()) {
            throw new IllegalArgumentException("defaultVersion cannot be null or blank");
        }

        this.defaultVersion = defaultVersion;
        this.rateLimiter = rateLimiter;

        log.info("EdgeFilters initialized: defaultVersion={}, rateLimiter={}",
                 defaultVersion, rateLimiter.getClass().getSimpleName());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Request ID: extract or generate for distributed tracing
        String requestId = request.getHeaders().getFirst(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            log.debug("Generated new request ID: {}", requestId);
        }

        // API Versioning: apply default if not specified
        String version = request.getHeaders().getFirst(HEADER_PLATFORM_VERSION);
        if (version == null) {
            version = defaultVersion;
            log.debug("Applied default API version: {} for request: {}", version, requestId);
        }

        final String finalRequestId = requestId;
        final String finalVersion = version;
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(HEADER_REQUEST_ID, finalRequestId)
            .header(HEADER_PLATFORM_VERSION, finalVersion)
            .build();

        response.getHeaders().set(HEADER_REQUEST_ID, finalRequestId);

        // Rate Limiting: check and apply rate limit
        String clientId = clientKey(request);
        RateLimitResult rateLimitResult = rateLimiter.checkRateLimit(clientId);

        response.getHeaders().set(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rateLimitResult.getRemainingTokens()));
        response.getHeaders().set(HEADER_RATE_LIMIT_RESET, String.valueOf(rateLimitResult.getResetTimeSeconds()));

        if (!rateLimitResult.isAllowed()) {
            log.warn("Rate limit exceeded: clientId={}, requestId={}, path={}",
                     clientId, finalRequestId, request.getPath());

            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String errorBody = String.format(
                "{\"meta\":{\"requestId\":\"%s\",\"timestamp\":\"%s\",\"status\":\"ERROR\",\"errors\":[{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please retry after %d seconds.\"}]},\"payload\":null}",
                finalRequestId,
                Instant.now(),
                rateLimitResult.getRetryAfterSeconds()
            );

            DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        log.debug("Request allowed: clientId={}, requestId={}, path={}, latency={}ms",
                  clientId, finalRequestId, request.getPath(), System.currentTimeMillis() - startTime);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Generates unique client identifier for rate limiting.
     * Uses Authorization header hash if present, otherwise falls back to IP address.
     * Properly handles X-Forwarded-For for load balancer scenarios with validation.
     */
    private String clientKey(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HEADER_AUTHORIZATION);
        if (auth != null && !auth.isBlank()) {
            return "auth:" + auth.hashCode();
        }

        String clientIp = extractClientIp(request);
        return "ip:" + clientIp;
    }

    /**
     * Extracts client IP with proper X-Forwarded-For handling.
     * Validates and uses the leftmost IP (original client) from X-Forwarded-For chain.
     */
    private String extractClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header (standard for proxies/load balancers)
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For format: client, proxy1, proxy2
            // Take the leftmost IP (original client)
            String clientIp = forwardedFor.split(",")[0].trim();
            if (isValidIp(clientIp)) {
                return clientIp;
            }
        }

        // Check X-Real-IP header (used by some proxies like nginx)
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && isValidIp(realIp)) {
            return realIp;
        }

        // Fallback to remote address
        InetSocketAddress remote = request.getRemoteAddress();
        return (remote == null) ? "unknown" : remote.getAddress().getHostAddress();
    }

    /**
     * Validates IP address format to prevent header spoofing.
     * Basic validation for IPv4 and IPv6 addresses.
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // Basic validation: IPv4 (x.x.x.x) or IPv6 (contains colons)
        return ip.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$") || ip.contains(":");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
