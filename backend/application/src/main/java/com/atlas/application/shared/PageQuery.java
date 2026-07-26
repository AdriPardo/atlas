package com.atlas.application.shared;

public record PageQuery(int page, int size, String sort) {

    public PageQuery {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
        if (sort == null || sort.isBlank()) {
            sort = "createdAt,desc";
        }
    }
}
