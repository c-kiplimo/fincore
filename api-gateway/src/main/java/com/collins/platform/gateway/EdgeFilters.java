package com.collins.platform.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Edge concerns: request id, default API version (Stripe-style date versions) and a simple per-client
 * token-bucket rate limit. In production a shared limiter (Redis) would replace the in-memory one.
 */
@Component
public class EdgeFilters implements GlobalFilter, Ordered {

    private final Map<String, long[]> buckets = new ConcurrentHashMap<>(); // key -> {tokens*1000, lastRefillMillis}
    private final int requestsPerSecond;
    private final String defaultVersion;

    public EdgeFilters(@Value("${gateway.rate-limit.requests-per-second:50}") int requestsPerSecond,
                       @Value("${gateway.api-version:2026-10-01}") String defaultVersion) {
        this.requestsPerSecond = requestsPerSecond;
        this.defaultVersion = defaultVersion;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getHeaders().getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String version = request.getHeaders().getFirst("Platform-Version");
        String finalId = requestId;
        ServerHttpRequest mutated = request.mutate()
            .header("X-Request-Id", finalId)
            .header("Platform-Version", version == null ? defaultVersion : version)
            .build();
        exchange.getResponse().getHeaders().set("X-Request-Id", finalId);

        if (!allow(clientKey(request))) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"meta\":{\"requestId\":\"" + finalId + "\",\"timestamp\":\"" + Instant.now()
                + "\",\"status\":\"ERROR\",\"errors\":[{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}]},\"payload\":null}";
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String clientKey(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth != null) {
            return "auth:" + auth.hashCode();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return "ip:" + (remote == null ? "unknown" : remote.getAddress().getHostAddress());
    }

    private boolean allow(String key) {
        long now = System.currentTimeMillis();
        long capacity = requestsPerSecond * 1000L;
        long[] bucket = buckets.computeIfAbsent(key, k -> new long[]{capacity, now});
        synchronized (bucket) {
            long refill = (now - bucket[1]) * requestsPerSecond;
            bucket[0] = Math.min(capacity, bucket[0] + refill);
            bucket[1] = now;
            if (bucket[0] >= 1000) {
                bucket[0] -= 1000;
                return true;
            }
            return false;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
