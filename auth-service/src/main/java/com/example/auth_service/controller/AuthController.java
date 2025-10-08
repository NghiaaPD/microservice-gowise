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

    /**
     * API Forgot Password - Send OTP
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email is required"));
            }

            // Send OTP to email
            boolean success = authService.sendPasswordResetOtp(email);

            if (success) {
                logger.info("Password reset OTP sent to: {}", email);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Password reset code has been sent to your email"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email not found"));
            }

        } catch (Exception e) {
            logger.error("Forgot password error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }

    /**
     * API Reset Password - Verify OTP and Update Password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String newPassword = request.get("new_password");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email is required"));
            }

            if (otp == null || otp.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "OTP is required"));
            }

            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "New password is required"));
            }

            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Password must be at least 6 characters"));
            }

            // Reset password with OTP validation
            boolean success = authService.resetPasswordWithOtp(email, otp, newPassword);

            if (success) {
                logger.info("Password reset successfully for: {}", email);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Password has been reset successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid or expired OTP"));
            }

        } catch (Exception e) {
            logger.error("Reset password error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }

    /**
     * API Validate OTP
     */
    @PostMapping("/validate-otp")
    public ResponseEntity<?> validateOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email is required"));
            }

            if (otp == null || otp.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "OTP is required"));
            }

            boolean isValid = authService.validateOtp(email, otp);

            if (isValid) {
                logger.info("OTP validated successfully for: {}", email);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "OTP is valid"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid or expired OTP"));
            }

        } catch (Exception e) {
            logger.error("Validate OTP error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }

    /**
     * API Change Password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            if (email == null || oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "email, oldPassword, and newPassword are required"));
            }

            boolean success = authService.changePassword(email, oldPassword, newPassword);

            if (success) {
                logger.info("Password changed successfully for user: {}", email);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Password changed successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message",
                                "Failed to change password. Please check your email or old password."));
            }

        } catch (Exception e) {
            logger.error("Change password error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Service temporarily unavailable"));
        }
    }
}
