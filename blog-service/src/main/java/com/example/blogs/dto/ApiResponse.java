package com.example.blogs.dto;

import java.util.Map;

public record ApiResponse<T>(
        int status,
        String message,
        T data,
        Map<String, Object> meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data, null);
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(200, "success", data, meta);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "created", data, null);
    }

    public static <T> ApiResponse<T> created(T data, Map<String, Object> meta) {
        return new ApiResponse<>(201, "created", data, meta);
    }
}
