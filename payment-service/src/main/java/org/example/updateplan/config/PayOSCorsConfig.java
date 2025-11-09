package org.example.updateplan.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures CORS for the PayOS endpoints so that the SPA on port 5173 can
 * create payment links directly against the payment service during local dev.
 */
@Configuration
public class PayOSCorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public PayOSCorsConfig(@Value("${payos.cors.allowed-origins:*}") String originsProperty) {
        this.allowedOrigins = Arrays.stream(originsProperty.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.isEmpty()
                ? new String[] { "*" }
                : allowedOrigins.toArray(new String[0]);

        registry.addMapping("/api/payos/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
