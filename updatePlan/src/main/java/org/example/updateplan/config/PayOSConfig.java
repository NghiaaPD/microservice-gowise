package org.example.updateplan.config;

import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import vn.payos.PayOS;

@Configuration
@EnableConfigurationProperties(PayOSProperties.class)
public class PayOSConfig {

    @Bean
    public PayOS payOS(PayOSProperties properties) {
        String clientId = Objects.requireNonNull(properties.getClientId(), "payos.client-id is required");
        String apiKey = Objects.requireNonNull(properties.getApiKey(), "payos.api-key is required");
        String checksumKey = Objects.requireNonNull(properties.getChecksumKey(), "payos.checksum-key is required");

        if (StringUtils.hasText(properties.getBaseUrl())) {
            return new PayOS(clientId, apiKey, checksumKey, properties.getBaseUrl());
        }

        return new PayOS(clientId, apiKey, checksumKey);
    }
}

