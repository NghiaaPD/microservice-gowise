package com.example.auth_service.util;

import com.example.auth_service.config.JwtProperties;
import com.example.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for JWT token operations in auth-service.
 * This must match the JwtUtil implementation in api-gateway.
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * Get signing key for JWT operations.
     * IMPORTANT: This must match the key generation in api-gateway.
     */
    private SecretKey getSigningKey() {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        logger.info("🔑 Creating signing key from secret: {}", jwtProperties.getSecret());
        logger.info("🔑 Secret key size: {} bits", jwtProperties.getSecret().getBytes().length * 8);
        return key;
    }

    /**
     * Generate JWT token for user.
     * Uses HS512 algorithm for enhanced security.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        logger.info("🔑 Generating JWT token for user: {}", user.getUsername());
        logger.info("🔑 Using SECRET_KEY: {}", jwtProperties.getSecret());
        logger.info("⏰ Token expires at: {}", expiryDate);

        String token = Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("role", user.getRole())
                .claim("username", user.getUsername()) // Add username for easier debugging
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        logger.info("✅ JWT token generated successfully!");
        logger.info("📝 Token length: {}", token.length());
        logger.info("📝 Token preview: {}", token.substring(0, Math.min(50, token.length())) + "...");

        return token;
    }

    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            logger.info("🔍 Validating JWT token...");
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            logger.info("✅ JWT token is valid!");
            return true;
        } catch (Exception e) {
            logger.error("❌ JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parse JWT token and extract claims.
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get user ID from JWT token.
     */
    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Get role from JWT token.
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * Get username from JWT token.
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }
}
