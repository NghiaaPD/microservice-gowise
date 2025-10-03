package com.example.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "JWT secret không được để trống")
    private String secret;

    @Positive(message = "JWT expiration phải là số dương")
    private long expiration;

    @Positive(message = "JWT refresh expiration phải là số dương")
    private long refreshExpiration;

    public JwtProperties() {
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
