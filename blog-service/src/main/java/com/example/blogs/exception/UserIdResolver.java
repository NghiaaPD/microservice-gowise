package com.example.blogs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class UserIdResolver {
    private UserIdResolver() {}

    public static UUID requireUserId(String header) {
        if (!StringUtils.hasText(header)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X-User-Id header");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID in X-User-Id");
        }
    }
}
