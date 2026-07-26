package com.atlas.application.shared;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements, String sort) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        return new PageResult<>(content, page, size, totalElements, totalPages, sort);
    }
}
