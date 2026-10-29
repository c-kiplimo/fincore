package com.collins.platform.commons.api;

import java.util.List;

/** A list payload with simple paging information. */
public record PageResult<T>(List<T> items, int limit, int offset, int count) {

    public static <T> PageResult<T> of(List<T> items, int limit, int offset) {
        return new PageResult<>(items, limit, offset, items.size());
    }
}
