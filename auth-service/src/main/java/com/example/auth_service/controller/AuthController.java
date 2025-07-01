package com.example.auth_service.controller;

import com.example.auth_service.entity.User;
import com.example.auth_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * API Sign Up
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");

            // Kiểm tra email đã tồn tại
            if (authService.existsByEmail(email)) {
                logger.warn("Signup failed: Email already exists - {}", email);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email is already in use"));
            }

            // Kiểm tra username đã tồn tại
            if (authService.existsByUsername(username)) {
                logger.warn("Signup failed: Username already exists - {}", username);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Username is already taken"));
            }

            User user = authService.createUser(username, email, password);
            logger.info("User registered successfully: {}", username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Registration successful! Please check your email to verify your account.",
                    "userId", user.getId().toString()));
        } catch (Exception e) {
            logger.error("Signup error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }

    /**
     * API Sign In
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> request) {
        try {
            String login = request.get("login");
            String password = request.get("password");

            Optional<User> userOpt = authService.findByEmail(login);
            if (userOpt.isEmpty()) {
                userOpt = authService.findByUsername(login);
            }

            if (userOpt.isEmpty()) {
                logger.warn("Signin failed: User not found - {}", login);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid login credentials"));
            }

            User user = userOpt.get();

            if (!authService.validatePassword(user, password)) {
                logger.warn("Signin failed: Invalid password for user - {}", login);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid login credentials"));
            }

            if (!user.getIsActive()) {
                logger.warn("Signin failed: Inactive account - {}", login);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Account is inactive"));
            }

            // Tạo JWT token và refresh token
            Map<String, String> tokens = authService.generateTokens(user);
            logger.info("User signed in successfully: {}", login);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful!",
                    "accessToken", tokens.get("accessToken"),
                    "refreshToken", tokens.get("refreshToken"),
                    "user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "role", user.getRole())));
        } catch (Exception e) {
            logger.error("Signin error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }

    /**
     * API Account Verification
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam String token) {
        try {
            boolean verified = authService.verifyAccount(token);

            if (verified) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Account verified successfully! You can now login."));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid or expired verification token"));
            }
        } catch (Exception e) {
            logger.error("Account verification error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * API Refresh Token - làm mới access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Refresh token is required"));
            }

            Map<String, String> tokens = authService.refreshAccessToken(refreshToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Token refreshed successfully!",
                    "accessToken", tokens.get("accessToken"),
                    "refreshToken", tokens.get("refreshToken")));

        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage(), e);
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Invalid or expired refresh token"));
        }
    }

    /**
     * API Logout - Xóa refresh token theo userId
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String userIdStr = request.get("userId");

            if (userIdStr == null || userIdStr.trim().isEmpty()) {
                logger.warn("Logout failed: No userId provided");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "UserId is required"));
            }

            UUID userId;
            try {
                userId = UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Logout failed: Invalid UUID format - {}", userIdStr);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid userId format"));
            }

            // Xóa tất cả refresh token của user này
            int deletedCount = authService.deleteAllRefreshTokensByUserId(userId);

            if (deletedCount > 0) {
                logger.info("User {} logged out successfully, deleted {} refresh tokens", userId, deletedCount);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Logged out successfully",
                        "deletedTokens", deletedCount));
            } else {
                logger.warn("Logout: No refresh tokens found for user {}", userId);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "User already logged out or no active sessions",
                        "deletedTokens", 0));
            }

        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }
}
