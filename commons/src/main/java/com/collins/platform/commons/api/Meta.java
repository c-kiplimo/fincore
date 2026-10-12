package com.collins.platform.commons.api;

import java.time.Instant;
import java.util.List;

/**
 * Response metadata. {@code status} is "SUCCESS" or "ERROR"; {@code requestId} is the correlation id
 * that also appears in logs and traces, so one id follows a request across every service.
 */
public record Meta(String requestId, Instant timestamp, String status, String path, List<ApiError> errors) {

    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
}
