package com.collins.platform.commons.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Every request body on the platform has the same shape: {@code {"payload": {...}}}.
 * Keeping the business data under "payload" leaves room for request-level fields later
 * (for example a client-supplied "meta") without breaking any API.
 */
public record ApiRequest<T>(@Valid @NotNull T payload) {

    public static <T> ApiRequest<T> of(T payload) {
        return new ApiRequest<>(payload);
    }
}
