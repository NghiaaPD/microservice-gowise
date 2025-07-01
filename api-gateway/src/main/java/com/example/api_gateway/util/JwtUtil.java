package com.example.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for JWT token operations.
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // This MUST match the secret key used in auth-service exactly
    private static final String SECRET_KEY = "abBsuoiqUhur/YMhUwoHWve5TCYAKA4GXLGm0Cwxg+9YaQvdiQYrO9sMBY9E9ZSiN+vNUPS77SwZ/f9H3I4eaw==";

    private final SecretKey key;

    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes()); // Match auth-service: no charset specified
        logger.info("🔑 JwtUtil initialized with SECRET_KEY: {}", SECRET_KEY);
        logger.info("🔑 SecretKey algorithm: {}", key.getAlgorithm());
        logger.info("🔑 SecretKey size: {} bits", SECRET_KEY.getBytes().length * 8);
    }

    /**
     * Parse JWT token and extract claims.
     *
     * @param token JWT token
     * @return Claims object containing token data
     */
    public Claims parseToken(String token) {
        try {
            logger.info("🔍 Attempting to parse JWT token...");
            logger.info("🔑 Using SECRET_KEY: {}", SECRET_KEY);
            logger.info("📝 Token to parse: {}", token.length() > 50 ? token.substring(0, 50) + "..." : token);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            logger.info("✅ JWT token parsed successfully!");
            logger.info("📋 Claims: subject={}, expiration={}, issuer={}",
                    claims.getSubject(), claims.getExpiration(), claims.getIssuer());

            return claims;
        } catch (Exception e) {
            logger.error("❌ Error parsing JWT token: {}", e.getMessage());
            logger.error("🔍 Token details: length={}, prefix={}",
                    token.length(),
                    token.length() > 20 ? token.substring(0, 20) + "..." : token);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Check if JWT token is expired.
     *
     * @param claims Claims from JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    /**
     * Extract user information from JWT claims and format response.
     *
     * @param claims JWT claims
     * @return Map containing user info and expiration status
     */
    public Map<String, Object> formatTokenInfo(Claims claims) {
        Map<String, Object> response = new HashMap<>();

        // Basic token info
        response.put("subject", claims.getSubject());
        response.put("issuedAt", claims.getIssuedAt());
        response.put("expiration", claims.getExpiration());
        response.put("issuer", claims.getIssuer());

        // Check if expired
        boolean isExpired = isTokenExpired(claims);
        response.put("isExpired", isExpired);
        response.put("status", isExpired ? "EXPIRED" : "VALID");

        // Add custom claims if they exist
        Map<String, Object> customClaims = new HashMap<>();
        claims.forEach((key, value) -> {
            if (!key.equals("sub") && !key.equals("iat") && !key.equals("exp") && !key.equals("iss")) {
                customClaims.put(key, value);
            }
        });

        if (!customClaims.isEmpty()) {
            response.put("customClaims", customClaims);
        }

        // Add time until expiration (if not expired)
        if (!isExpired) {
            long timeUntilExpiration = claims.getExpiration().getTime() - System.currentTimeMillis();
            response.put("timeUntilExpirationMs", timeUntilExpiration);
            response.put("timeUntilExpirationMinutes", timeUntilExpiration / (1000 * 60));
        }

        return response;
    }
}
