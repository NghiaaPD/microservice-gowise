package com.example.auth_service.service;

import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    // Refresh token hết hạn sau 7 ngày
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Tạo refresh token mới cho user
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Xóa refresh token cũ của user (chỉ cho phép 1 refresh token tại một thời
        // điểm)
        refreshTokenRepository.deleteByUser(user);

        // Tạo refresh token mới
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRATION_DAYS);

        RefreshToken refreshToken = new RefreshToken(user, tokenValue, expiresAt);
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        logger.info("Created refresh token for user: {}, expires at: {}", user.getUsername(), expiresAt);
        return savedToken;
    }

    /**
     * Kiểm tra refresh token có hợp lệ không
     */
    public boolean isRefreshTokenValid(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isEmpty()) {
            logger.warn("Refresh token not found: {}", token);
            return false;
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Refresh token expired for user: {}", refreshToken.getUser().getUsername());
            // Xóa token hết hạn
            refreshTokenRepository.delete(refreshToken);
            return false;
        }

        return true;
    }

    /**
     * Lấy user từ refresh token
     */
    public Optional<User> getUserFromRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        // Kiểm tra token hết hạn
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            return Optional.empty();
        }

        return Optional.of(refreshToken.getUser());
    }

    /**
     * Xóa refresh token (logout)
     */
    @Transactional
    public void deleteRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);

        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            logger.info("Deleting refresh token for user: {}", refreshToken.getUser().getUsername());
            refreshTokenRepository.delete(refreshToken);
        }
    }

    /**
     * Xóa tất cả refresh token của user (logout all devices)
     */
    @Transactional
    public void deleteAllRefreshTokensForUser(User user) {
        logger.info("Deleting all refresh tokens for user: {}", user.getUsername());
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Làm mới refresh token (tạo token mới, xóa token cũ)
     */
    @Transactional
    public RefreshToken refreshRefreshToken(String oldToken) {
        Optional<User> userOpt = getUserFromRefreshToken(oldToken);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user = userOpt.get();

        // Xóa token cũ
        deleteRefreshToken(oldToken);

        // Tạo token mới
        return createRefreshToken(user);
    }

    /**
     * Xóa tất cả refresh token của user
     */
    @Transactional
    public int deleteAllRefreshTokensByUserId(UUID userId) {
        try {
            logger.info("🗑️ Attempting to delete refresh tokens for user: {}", userId);

            // Đầu tiên, tìm tất cả refresh token của user
            List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
            int count = tokens.size();

            logger.info("🔍 Found {} refresh tokens for user {}", count, userId);

            if (count > 0) {
                // Xóa bằng cách sử dụng deleteByUserId
                refreshTokenRepository.deleteByUserId(userId);
                logger.info("✅ Successfully deleted {} refresh tokens for user {}", count, userId);
            } else {
                logger.info("ℹ️ No refresh tokens found for user {}", userId);
            }

            return count;
        } catch (Exception e) {
            logger.error("❌ Error deleting all refresh tokens for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }
}
