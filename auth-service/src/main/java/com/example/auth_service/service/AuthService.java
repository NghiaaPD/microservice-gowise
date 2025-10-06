package com.example.auth_service.service;

import com.example.auth_service.config.JwtProperties;
import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
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
    private JwtProperties jwtProperties;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

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
     * Tạo JWT token chứa userId, email, role và thời gian hết hạn
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("role", user.getRole())
                .claim("email", user.getEmail())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
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
     * Tạo refresh token cho user
     */
    public RefreshToken createRefreshToken(User user) {
        // Xóa tất cả refresh token cũ của user (nếu có nhiều thiết bị, comment dòng
        // này)
        // refreshTokenRepository.deleteByUser(user);

        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshExpiration() / 1000);

        RefreshToken refreshToken = new RefreshToken(user, tokenValue, expiresAt);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validate và rotate refresh token
     */
    @Transactional
    public RefreshToken validateAndRotateRefreshToken(String tokenValue) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("Refresh token không tồn tại");
        }

        RefreshToken oldToken = tokenOpt.get();

        // Kiểm tra hết hạn
        if (oldToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(oldToken);
            throw new RuntimeException("Refresh token đã hết hạn");
        }

        User user = oldToken.getUser();

        // Xóa token cũ
        refreshTokenRepository.delete(oldToken);

        // Tạo token mới
        return createRefreshToken(user);
    }

    /**
     * Xóa refresh token (logout)
     */
    public void deleteRefreshToken(String tokenValue) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);
        tokenOpt.ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Cleanup expired refresh tokens
     */
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        LocalDateTime now = LocalDateTime.now();
        long expiredCount = refreshTokenRepository.countByExpiresAtBefore(now);

        if (expiredCount > 0) {
            refreshTokenRepository.deleteByExpiresAtBefore(now);
            logger.info("Cleaned up {} expired refresh tokens", expiredCount);
        }
    }

    /**
     * Generate 6-digit OTP
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(otp);
    }

    /**
     * Send password reset OTP to user email
     */
    public boolean sendPasswordResetOtp(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return false; // User not found
        }

        User user = userOpt.get();

        // Generate OTP and set expiry (10 minutes)
        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        // Send OTP email
        try {
            emailService.sendPasswordResetEmail(email, user.getUsername(), otp);
            logger.info("Password reset OTP sent to: {}", email);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send password reset OTP to {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Reset password with OTP validation
     */
    public boolean resetPasswordWithOtp(String email, String otp, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return false; // User not found
        }

        User user = userOpt.get();

        // Validate OTP
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            logger.warn("Invalid OTP for password reset: {}", email);
            return false;
        }

        // Check OTP expiry
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Expired OTP for password reset: {}", email);
            return false;
        }

        // Update password and clear OTP
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);

        userRepository.save(user);

        logger.info("Password reset successfully for: {}", email);
        return true;
    }

    /**
     * Validate OTP without resetting password
     */
    public boolean validateOtp(String email, String otp) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return false; // User not found
        }

        User user = userOpt.get();

        // Validate OTP
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            logger.warn("Invalid OTP validation attempt: {}", email);
            return false;
        }

        // Check OTP expiry
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Expired OTP validation attempt: {}", email);
            return false;
        }

        logger.info("OTP validated successfully for: {}", email);
        return true;
    }

    /**
     * Change password for a user by email
     */
    public boolean changePassword(String email, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            logger.warn("User not found for password change: {}", email);
            return false;
        }

        User user = userOpt.get();

        // Validate old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            logger.warn("Invalid old password for user: {}", email);
            return false;
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password changed successfully for user: {}", email);
        return true;
    }
}
