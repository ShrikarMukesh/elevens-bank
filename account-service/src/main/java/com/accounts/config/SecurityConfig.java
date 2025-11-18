package com.accounts.config;

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.List;

@Configuration // SRP: This class is responsible ONLY for configuring Spring Security.
@EnableAutoConfiguration(exclude = {UserDetailsServiceAutoConfiguration.class})
// SRP: Excluding default user-details config to keep this class focused on custom security setup.
public class SecurityConfig {

    @Bean
    // DIP: Higher-level components depend on this SecurityFilterChain abstraction, not its implementation.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // SRP: Configures CSRF only, separate from other security responsibilities.

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // SRP: Delegates CORS configuration to a dedicated method → separation of concerns.

                .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/api/accounts/status").permitAll()
                                .requestMatchers("/actuator/**").permitAll()
                                .anyRequest().permitAll()
                        // SRP: Only responsible for defining authorization rules (no business logic).
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // SRP: Dedicated method solely responsible for CORS configuration (no mixing with security rules).
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:3000"));
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}
