package com.parklock.parklock_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/parking/**", "/api/v1/gates/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
            
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            String publicKeyPEM = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp+kIBy6N19QhK4eNNRYyR6UqkWnw4NitblxaVjlr04b5kKI9aYLLlsTSH9sCQlNp4xN4Ar5/AnB2PNrAYL90MQy+CtMRJkIWqBp8rUX2UHWseMZRyqYod5sUW10N0/dAgco05vIMSDYYCmLY06ipY/WjSHWs7OyhCEyktILFcHEOgRxXWyeG/tpg6hq2T4HI995j7CFjGGmP3+zRVrvPkNIj/u9bSUSn4wU+GSS4qwQqQzHTWNrHX9aVZm6hU3iJQryRTPl2ZcHZZ8JF4w01AK38anXdlqiLX4Oq4JVRA6hj+4FPQpyJKm7QSwTUQkHjhnvnZeeJrqNj58ialY6jTwIDAQAB";
            
            byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            
            // Only validate token expiration timestamp locally, ignoring strict issuer URL checks
            OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
            jwtDecoder.setJwtValidator(withTimestamp);
            
            return jwtDecoder;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWT Decoder with Clerk Public Key", e);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}