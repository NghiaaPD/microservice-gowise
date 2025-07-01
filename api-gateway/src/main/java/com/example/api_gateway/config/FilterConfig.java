package com.example.api_gateway.config;

import com.example.api_gateway.filter.JwtTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JWT Token Filter.
 */
@Configuration
public class FilterConfig {

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    /**
     * Register JWT Token Filter for /v1/** paths.
     *
     * @return FilterRegistrationBean for JWT filter
     */
    @Bean
    public FilterRegistrationBean<JwtTokenFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtTokenFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(jwtTokenFilter);
        registrationBean.addUrlPatterns("/v1/*");
        registrationBean.setOrder(2); // Set after CORS filter (order 1)
        registrationBean.setName("JwtTokenFilter");

        return registrationBean;
    }
}
