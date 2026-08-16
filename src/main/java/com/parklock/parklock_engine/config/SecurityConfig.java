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
                // Allow public endpoints (like your parking grid and gate controls)
                .requestMatchers("/api/parking/**", "/api/gates/**", "/error").permitAll()
                
                // Lock down everything else so it requires a valid Clerk JWT token
                .anyRequest().authenticated()
            )
            
            // 4. Enable OAuth2 Resource Server support for validating JWTs from Clerk
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
            
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