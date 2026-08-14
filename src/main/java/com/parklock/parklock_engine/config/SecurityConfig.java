package com.parklock.parklock_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Tell Spring Security to use the CORS policy defined below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. Disable CSRF because we are building a stateless REST API
            .csrf(csrf -> csrf.disable()) 
            
            // 3. Configure route permissions
            .authorizeHttpRequests(auth -> auth
                // Temporarily permit all API routes so Vercel can fetch data 
                // until we implement the JWT token system!
                .requestMatchers("/api/**").permitAll() 
                .anyRequest().authenticated()
            );
            
            // NOTE: We will inject our JWT token filter here in the next step!
            
        return http.build();
    }

    /*
     * INTERVIEW DEFENSE POINT (Security CORS):
     * By defining the CorsConfigurationSource as a Bean here, we ensure Spring 
     * Security processes CORS preflight (OPTIONS) requests correctly before 
     * enforcing authentication. This prevents false-positive CORS errors on the frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Allow Vercel & Localhost
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}