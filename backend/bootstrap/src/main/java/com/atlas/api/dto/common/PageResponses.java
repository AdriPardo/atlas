package com.atlas.api.dto.common;

import com.atlas.application.shared.PageResult;

public final class PageResponses {

    private PageResponses() {}

    public static <T, R> PageResponse<R> from(PageResult<T> page, java.util.function.Function<T, R> mapper) {
        return new PageResponse<>(
                page.content().stream().map(mapper).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.sort());
    }
}
