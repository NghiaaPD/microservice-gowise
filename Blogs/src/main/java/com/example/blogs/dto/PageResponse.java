package com.example.blogs.dto;

import org.hibernate.jdbc.Expectation;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long numberOfElements,
        long totalElements
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumberOfElements(),
                page.getTotalElements()
        );
    }
}