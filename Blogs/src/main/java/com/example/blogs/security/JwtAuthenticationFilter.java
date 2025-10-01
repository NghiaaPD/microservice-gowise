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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final boolean enableHeaderFallback;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean authenticated = false;

        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                JwtUtils.ParsedToken parsed = jwtUtils.parseAndValidate(token);
                setAuthentication(parsed.userId(), parsed.roles());
                authenticated = true;
            } catch (Exception e) {
                // Token sai hoặc hết hạn => trả 401 JSON
                SecurityContextHolder.clearContext();

                ApiError body = new ApiError(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        "Invalid or expired token"
                );
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                new ObjectMapper().writeValue(response.getWriter(), body);
                return; // Dừng chain, không cho đi tiếp
            }
        }

        // Fallback: nếu bật & chưa authenticated & có X-User-Id -> set ROLE_USER
        if (!authenticated && enableHeaderFallback) {
            String userHeader = request.getHeader("X-User-Id");
            if (StringUtils.hasText(userHeader)) {
                try {
                    UUID uid = UUID.fromString(userHeader);
                    setAuthentication(uid, List.of(Role.USER.name()));
                } catch (IllegalArgumentException ignored) { }
            }
        }

        chain.doFilter(request, response);
    }

    private void setAuthentication(UUID userId, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .filter(Objects::nonNull)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        AbstractAuthenticationToken authToken = new AbstractAuthenticationToken(authorities) {
            @Override
            public Object getCredentials() { return ""; }
            @Override
            public Object getPrincipal() { return userId; }
            @Override
            public String getName() { return userId.toString(); }
        };
        authToken.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        System.out.println("[JWT] user=" + userId + " authorities=" +
                authorities.stream().map(a -> a.getAuthority()).toList());
    }
}
