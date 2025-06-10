package com.example.demo.service;

import com.example.demo.entity.User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service for user operations.
 */
@Service
public final class UserService {

    /**
     * Get all users.
     * 
     * @return list of users
     */
    public List<User> getAllUsers() {
        return Arrays.asList(
                new User(1L, "Alice", "alice@example.com"),
                new User(2L, "Bob", "bob@example.com"),
                new User(UserService.CHARLIE_ID, "Charlie", "charlie@example.com"));
    }

    /**
     * Constant for Charlie's user id.
     */
    private static final long CHARLIE_ID = 3L;
}