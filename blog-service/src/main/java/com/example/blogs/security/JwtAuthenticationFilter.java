package com.example.blogs.security;

import com.example.blogs.Utils.JwtUtils;
import com.example.blogs.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final boolean enableHeaderFallback;
    private static final Set<String> KNOWN_ROLES = Arrays.stream(Role.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean authenticated = false;

        boolean tokenInvalid = false;

        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                JwtUtils.ParsedToken parsed = jwtUtils.parseAndValidate(token);
                setAuthentication(parsed.userId(), parsed.roles());
                authenticated = true;
            } catch (Exception e) {
                System.out.println(
                        "[JWT Error] Failed to parse token: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
                tokenInvalid = true;
                SecurityContextHolder.clearContext();
            }
        }

        // Fallback: nếu bật & chưa authenticated & có header phù hợp
        if (!authenticated && enableHeaderFallback) {
            authenticated = tryHeaderFallback(request);
            // Nếu fallback thành công thì bỏ qua lỗi JWT
            if (authenticated) {
                tokenInvalid = false;
                System.out.println("[JWT] Fallback authentication successful via headers");
            }
        }

        if (!authenticated && tokenInvalid) {
            ApiError body = new ApiError(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Invalid or expired token");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            new ObjectMapper().writeValue(response.getWriter(), body);
            return;
        }

        chain.doFilter(request, response);
    }

    private void setAuthentication(UUID userId, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .filter(Objects::nonNull)
                .map(r -> {
                    // Chuẩn hóa role thành ROLE_UPPERCASE cho Spring Security
                    if (r.toUpperCase().startsWith("ROLE_")) {
                        return r.toUpperCase();
                    } else {
                        // Thêm tiền tố ROLE_ và chuyển thành chữ HOA
                        return "ROLE_" + r.toUpperCase();
                    }
                })
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        AbstractAuthenticationToken authToken = new AbstractAuthenticationToken(authorities) {
            @Override
            public Object getCredentials() {
                return "";
            }

            @Override
            public Object getPrincipal() {
                return userId;
            }

            @Override
            public String getName() {
                return userId.toString();
            }
        };
        authToken.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        System.out.println("[JWT] user=" + userId + " authorities=" +
                authorities.stream().map(a -> a.getAuthority()).toList());
    }

    private boolean tryHeaderFallback(HttpServletRequest request) {
        String userHeader = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(userHeader)) {
            return false;
        }
        try {
            UUID uid = UUID.fromString(userHeader);
            List<String> fallbackRoles = parseRolesHeader(request.getHeader("X-User-Roles"));
            if (fallbackRoles.isEmpty()) {
                fallbackRoles = List.of(Role.user.name());
            }
            setAuthentication(uid, fallbackRoles);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private List<String> parseRolesHeader(String header) {
        if (!StringUtils.hasText(header)) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> {
                    // Loại bỏ tiền tố ROLE_ nếu có (không phân biệt hoa thường)
                    if (role.toUpperCase().startsWith("ROLE_")) {
                        return role.substring(5);
                    }
                    return role;
                })
                .map(String::toLowerCase) // Chuyển thành chữ thường
                .filter(KNOWN_ROLES::contains)
                .distinct()
                .toList();
    }
}
