package com.atlas.api.dto.common;

import java.util.List;

public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, String sort) {}
