package com.atlas.infrastructure.persistence.jpa;

import com.atlas.application.shared.PageQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableFactory {

    private PageableFactory() {}

    public static Pageable from(PageQuery pageQuery) {
        String[] parts = pageQuery.sort().split(",");
        String property = parts[0].trim();
        Sort.Direction direction =
                parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
        return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(direction, property));
    }
}
