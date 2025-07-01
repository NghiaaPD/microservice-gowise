package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token validation filter for /v1/** endpoints.
 */
@Component
public class JwtTokenFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Thêm CORS headers cho tất cả responses
        addCorsHeaders(request, response);

        String requestUri = request.getRequestURI();
        logger.info("Processing request: {} {}", request.getMethod(), requestUri);

        // Handle preflight OPTIONS requests
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Only process /v1/** endpoints
        if (!requestUri.startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token from custom header, Authorization header, or cookies
            String jwtToken = extractJwtToken(request);

            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                sendErrorResponse(response, "JWT token not found in headers or cookies", 401);
                return;
            }

            // Parse and validate JWT token
            Claims claims = jwtUtil.parseToken(jwtToken);

            // Format token information
            Map<String, Object> tokenInfo = jwtUtil.formatTokenInfo(claims);

            // Check if token is expired
            boolean isExpired = (Boolean) tokenInfo.get("isExpired");

            if (isExpired) {
                // Token đã hết hạn - trả về thông tin JWT đã decode
                Map<String, Object> expiredResponse = new HashMap<>();
                expiredResponse.put("message", "JWT Token has EXPIRED");
                expiredResponse.put("requestPath", requestUri);
                expiredResponse.put("method", request.getMethod());
                expiredResponse.put("tokenInfo", tokenInfo);
                expiredResponse.put("warning", "⚠️ JWT Token has EXPIRED! Please refresh your token.");

                sendErrorResponse(response, "Token expired", 401);
                return;
            } else {
                // Token hợp lệ - forward request đến backend service
                logger.info("✅ JWT Token is valid, forwarding request to backend service");

                // Add user info to request headers for backend services
                request.setAttribute("username", claims.getSubject());
                request.setAttribute("roles", claims.get("roles", String.class));

                // Forward to backend service
                filterChain.doFilter(request, response);
                return;
            }

        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
            sendErrorResponse(response, "Invalid JWT token: " + e.getMessage(), 401);
        }
    }

    /**
     * Extract JWT token from multiple sources in order of preference:
     * 1. Custom header 'jwt_token'
     * 2. Authorization header 'Bearer <token>'
     * 3. Cookie 'jwt_token'
     *
     * @param request HTTP request
     * @return JWT token string or null if not found
     */
    private String extractJwtToken(HttpServletRequest request) {
        // 1. Try custom header first (FE is using this)
        String customHeaderToken = request.getHeader("jwt_token");
        if (customHeaderToken != null && !customHeaderToken.trim().isEmpty()) {
            logger.info("✅ JWT token found in custom header 'jwt_token'!");
            return customHeaderToken.trim();
        }

        // 2. Try Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            logger.info("✅ JWT token found in Authorization header!");
            return token;
        }

        // 3. Try cookies as fallback
        Cookie[] cookies = request.getCookies();
        logger.info("🍪 Total cookies received: {}", cookies != null ? cookies.length : 0);

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                logger.info("🍪 Cookie found: name='{}', value='{}'",
                        cookie.getName(),
                        cookie.getValue().length() > 50 ? cookie.getValue().substring(0, 50) + "..."
                                : cookie.getValue());

                if ("jwt_token".equals(cookie.getName())) {
                    logger.info("✅ JWT token found in cookie!");
                    return cookie.getValue();
                }
            }
        }

        logger.warn("❌ JWT token NOT found in any source (custom header, Authorization header, or cookies)!");
        return null;
    }

    /**
     * Send error response.
     *
     * @param response   HTTP response
     * @param message    Error message
     * @param statusCode HTTP status code
     * @throws IOException if response writing fails
     */
    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Add CORS headers to response.
     *
     * @param request  HTTP request
     * @param response HTTP response
     */
    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        // Allow specific origins or all origins for development
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers",
                "Origin, Content-Type, Accept, Authorization, X-Requested-With, jwt_token");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

}
