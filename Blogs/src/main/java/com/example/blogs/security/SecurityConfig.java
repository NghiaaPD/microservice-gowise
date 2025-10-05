package com.example.blogs.security;

import com.example.blogs.Utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${security.jwt.base64-secret}")
    private String base64Secret;

    @Value("${security.jwt.ttl-seconds:3600}")
    private long ttlSeconds;

    @Value("${security.fallback-header-auth:false}")
    private boolean fallbackHeaderAuth;

    private final JsonAuthenticationEntryPoint authenticationEntryPoint; // 401
    private final JsonAccessDeniedHandler accessDeniedHandler;           // 403

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(base64Secret, ttlSeconds);
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtUtils jwtUtils) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtils, fallbackHeaderAuth);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers(HttpMethod.GET, "/api/posts/feed").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // còn lại
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
