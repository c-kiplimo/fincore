package com.collins.platform.commons.api;

import com.collins.platform.commons.web.RequestContext;

import java.time.Instant;
import java.util.List;

/**
 * Every response on the platform has the same shape: {@code {"meta": {...}, "payload": {...}}}.
 */
public record ApiResponse<T>(Meta meta, T payload) {

    public static <T> ApiResponse<T> ok(T payload) {
        Meta meta = new Meta(RequestContext.requestId(), Instant.now(), Meta.SUCCESS, RequestContext.path(), List.of());
        return new ApiResponse<>(meta, payload);
    }

    public static ApiResponse<Void> error(List<ApiError> errors) {
        Meta meta = new Meta(RequestContext.requestId(), Instant.now(), Meta.ERROR, RequestContext.path(), errors);
        return new ApiResponse<>(meta, null);
    }
}
