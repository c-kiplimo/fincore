package com.collins.platform.commons.api;

/**
 * One problem with a request. {@code field} is set for validation errors and null otherwise.
 */
public record ApiError(String code, String message, String field) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
