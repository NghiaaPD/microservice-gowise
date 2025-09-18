package com.example.auth_service.controller;

import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

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

            // Tạo JWT access token
            String accessToken = authService.generateToken(user);

            // Tạo refresh token
            RefreshToken refreshToken = authService.createRefreshToken(user);

            logger.info("User signed in successfully: {}", login);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful!",
                    "accessToken", accessToken,
                    "refreshToken", refreshToken.getToken(),
                    "expiresIn", 600, // 10 phút
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
     * API Refresh Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshTokenValue = request.get("refreshToken");

            if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Refresh token is required"));
            }

            // Validate và rotate refresh token
            RefreshToken newRefreshToken = authService.validateAndRotateRefreshToken(refreshTokenValue);

            // Tạo access token mới
            String newAccessToken = authService.generateToken(newRefreshToken.getUser());

            logger.info("Token refreshed successfully for user: {}", newRefreshToken.getUser().getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken.getToken(),
                    "expiresIn", 600)); // 10 phút

        } catch (RuntimeException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }

    /**
     * API Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshTokenValue = request.get("refreshToken");

            if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Refresh token is required"));
            }

            // Xóa refresh token
            authService.deleteRefreshToken(refreshTokenValue);

            logger.info("User logged out successfully");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logged out successfully"));

        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }
}
