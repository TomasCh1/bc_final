package com.basketball.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtTokenFilter jwtTokenFilter;
    private final boolean allowTestEndpoints;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         JwtTokenFilter jwtTokenFilter,
                         @Value("${app.security.allow-test-endpoints:false}") boolean allowTestEndpoints) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtTokenFilter = jwtTokenFilter;
        this.allowTestEndpoints = allowTestEndpoints;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for JWT (stateless)
            .cors(cors -> {}) // Enable CORS
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless JWT
            )
            .authorizeHttpRequests(auth -> {
                // Always allow browser CORS preflight requests
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                // Public endpoints
                auth.requestMatchers("/", "/css/**", "/js/**", "/pages/**", "/IMG/**").permitAll();
                auth.requestMatchers("/api/auth/**").permitAll();
                auth.requestMatchers("/api/health").permitAll();

                if (allowTestEndpoints) {
                    auth.requestMatchers("/api/test/**").permitAll();
                }

                // API endpoints require authentication
                auth.requestMatchers("/api/**").authenticated();

                // All other requests permitted (for frontend files)
                auth.anyRequest().permitAll();
            });

        // Add JWT token filter
        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

