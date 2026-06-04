package com.basketball.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {

    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private boolean allowCredentials = true;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        if (allowedOrigins != null) {
            config.setAllowedOrigins(normalizeOrigins(allowedOrigins));
        } else {
            config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080"
            ));
        }
        
        if (allowedMethods != null) {
            config.setAllowedMethods(allowedMethods);
        } else {
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        }
        
        if (allowedHeaders != null) {
            config.setAllowedHeaders(allowedHeaders);
        } else {
            config.setAllowedHeaders(List.of("*"));
        }
        
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(3600L); // 1 hour
        
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    // Getters and setters for ConfigurationProperties binding
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    private List<String> normalizeOrigins(List<String> origins) {
        return origins.stream()
            .filter(origin -> origin != null && !origin.isBlank())
            .flatMap(origin -> Arrays.stream(origin.split(",")))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .collect(Collectors.toList());
    }
}

