package com.example.blogs.security;

public enum Role {
    user, admin;

    // Cho phép tìm role không phân biệt hoa thường
    public static Role fromString(String role) {
        if (role == null)
            return null;
        try {
            return Role.valueOf(role.toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}