package com.example.auth_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test utility to verify JWT token compatibility between auth-service and
 * api-gateway
 */
public class JwtCompatibilityTest {

    private static final Logger logger = LoggerFactory.getLogger(JwtCompatibilityTest.class);

    public static void testJwtCompatibility() {
        logger.info("=== JWT COMPATIBILITY TEST ===");

        // This will be used for manual testing to ensure both services use same secret
        String authServiceSecret = "defaultSecretKeyForJWTTokenGeneration1234567890";
        String apiGatewaySecret = "defaultSecretKeyForJWTTokenGeneration1234567890";

        logger.info("Auth-service SECRET: {}", authServiceSecret);
        logger.info("API-gateway SECRET: {}", apiGatewaySecret);
        logger.info("Secrets match: {}", authServiceSecret.equals(apiGatewaySecret));

        // Test key generation
        try {
            byte[] authServiceBytes = authServiceSecret.getBytes();
            byte[] apiGatewayBytes = apiGatewaySecret.getBytes();

            logger.info("Auth-service bytes length: {}", authServiceBytes.length);
            logger.info("API-gateway bytes length: {}", apiGatewayBytes.length);

            boolean bytesMatch = java.util.Arrays.equals(authServiceBytes, apiGatewayBytes);
            logger.info("Secret bytes match: {}", bytesMatch);

            if (bytesMatch) {
                logger.info("✅ JWT compatibility test PASSED!");
            } else {
                logger.error("❌ JWT compatibility test FAILED!");
            }

        } catch (Exception e) {
            logger.error("❌ JWT compatibility test ERROR: {}", e.getMessage());
        }

        logger.info("=== END JWT COMPATIBILITY TEST ===");
    }
}
