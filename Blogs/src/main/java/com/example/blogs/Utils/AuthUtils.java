package com.example.blogs.Utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class AuthUtils {
    private AuthUtils() {}
    public static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object p = auth.getPrincipal();
        return (p instanceof UUID) ? (UUID) p : UUID.fromString(p.toString());
    }
}