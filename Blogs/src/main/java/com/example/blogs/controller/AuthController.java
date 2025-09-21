package com.example.blogs.controller;

import com.example.blogs.dto.ApiResponse;
import com.example.blogs.entity.User;
import com.example.blogs.repository.UserRepository;
import com.example.blogs.utils.JWTUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final JWTUtils jwtUtil;
    private final BCryptPasswordEncoder encoder;

    public AuthController(UserRepository userRepo, JWTUtils jwtUtil, BCryptPasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
    }

    // ===== Helpers =====
    private static String optString(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private static Set<User.Role> parseRoles(Object raw) {
        if (raw == null) return Collections.emptySet();

        // Nếu client gửi roles là mảng: ["ADMIN","USER"]
        if (raw instanceof Collection<?> col) {
            EnumSet<User.Role> set = EnumSet.noneOf(User.Role.class);
            for (Object item : col) {
                if (item != null) {
                    try {
                        set.add(User.Role.valueOf(String.valueOf(item).trim().toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return set;
        }

        // Nếu client gửi một string: "ADMIN"
        try {
            return EnumSet.of(User.Role.valueOf(String.valueOf(raw).trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Collections.emptySet();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, Object> req) {
        String username = optString(req.get("username"));
        String password = optString(req.get("password"));

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("username/password is required", 400));
        }

        if (userRepo.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Username already exists!", 400));
        }

        // Parse roles: chấp nhận String hoặc Collection
        Set<User.Role> roles = parseRoles(req.get("roles"));
        if (roles.isEmpty()) roles = EnumSet.of(User.Role.USER);

        User toSave = User.builder()
                .username(username.trim())
                .password(encoder.encode(password))
                .roles(roles)
                .build();

        User saved = userRepo.save(toSave);

        // không trả password
        Map<String, Object> data = Map.of(
                "id", saved.getId(),
                "username", saved.getUsername(),
                "roles", saved.getRoles()
        );

        return ResponseEntity.ok(ApiResponse.ok(data, "User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody Map<String, Object> req) {
        String username = optString(req.get("username"));
        String password = optString(req.get("password"));

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("username/password is required", 400));
        }

        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found", 401));
        }

        var user = userOpt.get();
        if (!encoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials", 401));
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );

        return ResponseEntity.ok(ApiResponse.ok(Map.of("token", token), "User logged in successfully!"));
    }
}
