package com.example.blogs.exception;

public record ApiError(
        int status,
        String error,
        String message
) {}