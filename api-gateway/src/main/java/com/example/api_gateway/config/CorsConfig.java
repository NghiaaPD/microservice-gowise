package com.example.api_gateway.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Cho phép specific origins (thay vì *)
        corsConfiguration.setAllowedOriginPatterns(Arrays.asList(
                "http://127.0.0.1:*",
                "http://localhost:*",
                "http://nghiapd.kiko-ecrux.ts.net:*",
                "http://635b-171-235-191-90.ngrok-free.app/"));

        // Cho phép tất cả HTTP methods
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Cho phép tất cả headers
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));

        // Cho phép credentials (cookies, authorization headers) - QUAN TRỌNG!
        corsConfiguration.setAllowCredentials(true);

        // Expose headers để frontend có thể đọc
        corsConfiguration.setExposedHeaders(Arrays.asList("*"));

        // Thời gian cache preflight request (giây)
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        CorsFilter corsFilter = new CorsFilter(source);

        // Tạo FilterRegistrationBean để set order
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(corsFilter);
        registrationBean.setOrder(1); // Chạy đầu tiên, trước JWT filter
        registrationBean.setName("CorsFilter");

        return registrationBean;
    }
}
