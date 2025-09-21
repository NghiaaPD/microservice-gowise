package com.example.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TokenCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);

    @Autowired
    private AuthService authService;

    /**
     * Cleanup expired refresh tokens every hour
     */
    @Scheduled(fixedRate = 3600000) // 3600000 ms = 1 hour
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired refresh tokens...");
        authService.cleanupExpiredRefreshTokens();
        logger.info("Finished cleanup of expired refresh tokens");
    }
}