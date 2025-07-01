package com.example.auth_service.service;

import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    public User createUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("user");
        user.setIsActive(false); // Mặc định không active

        // Tạo verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setTokenExpiry(LocalDateTime.now().plusHours(24)); // Token hết hạn sau 24h

        User savedUser = userRepository.save(user);

        // Gửi email xác nhận - WRAP trong try-catch
        try {
            emailService.sendVerificationEmail(email, username, verificationToken);
            logger.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", email, e.getMessage());
            // KHÔNG throw exception - để signup process tiếp tục thành công
        }

        return savedUser;
    }

    public boolean verifyAccount(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Kiểm tra token hết hạn
        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Kích hoạt tài khoản
        user.setIsActive(true);
        user.setVerificationToken(null); // Xóa token sau khi verify
        user.setTokenExpiry(null);
        userRepository.save(user);

        return true;
    }

    /**
     * Tạo JWT token chỉ chứa userId, role và thời gian hết hạn
     */
    public String generateToken(User user) {
        return jwtUtil.generateToken(user);
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public String getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }

    public String getRoleFromToken(String token) {
        return jwtUtil.getRoleFromToken(token);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean validatePassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPassword()); // Sửa từ getPasswordHash() thành getPassword()
    }

    /**
     * Tạo access token và refresh token cho user
     */
    public Map<String, String> generateTokens(User user) {
        String accessToken = jwtUtil.generateToken(user);
        com.example.auth_service.entity.RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken.getToken());

        logger.info("Generated tokens for user: {}", user.getUsername());
        return tokens;
    }

    /**
     * Làm mới access token bằng refresh token
     */
    public Map<String, String> refreshAccessToken(String refreshToken) {
        if (!refreshTokenService.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        Optional<User> userOpt = refreshTokenService.getUserFromRefreshToken(refreshToken);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found for refresh token");
        }

        User user = userOpt.get();

        // Kiểm tra user vẫn active
        if (!user.getIsActive()) {
            refreshTokenService.deleteRefreshToken(refreshToken);
            throw new RuntimeException("User account is inactive");
        }

        // Tạo access token mới
        String newAccessToken = jwtUtil.generateToken(user);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", refreshToken); // Giữ nguyên refresh token

        logger.info("Refreshed access token for user: {}", user.getUsername());
        return tokens;
    }

    /**
     * Logout user - xóa refresh token
     */
    public void logout(String refreshToken) {
        refreshTokenService.deleteRefreshToken(refreshToken);
        logger.info("User logged out, refresh token deleted");
    }

    /**
     * Logout từ tất cả thiết bị - xóa tất cả refresh token của user
     */
    public void logoutAllDevices(User user) {
        refreshTokenService.deleteAllRefreshTokensForUser(user);
        logger.info("User logged out from all devices: {}", user.getUsername());
    }

    /**
     * Xóa tất cả refresh token theo userId
     */
    public int deleteAllRefreshTokensByUserId(UUID userId) {
        return refreshTokenService.deleteAllRefreshTokensByUserId(userId);
    }
}
