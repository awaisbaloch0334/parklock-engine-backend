package com.parklock.parklock_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF because we are building a stateless REST API
            .csrf(csrf -> csrf.disable()) 
            
            // Configure route permissions
            .authorizeHttpRequests(auth -> auth
                // 1. Let the frontend gate controls and grid polling work without a password
                .requestMatchers("/api/v1/parking/**").permitAll() 
                
                // 2. Lock down the new Admin Dashboard routes
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") 
                
                // 3. Anything else requires authentication
                .anyRequest().authenticated()
            );
            
            // NOTE: We will inject our JWT token filter here in the next step!
            
        return http.build();
    }
}