package com.accounts.config;

import com.accounts.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration // SRP: This class is responsible ONLY for configuring Spring Security.
public class SecurityConfig {

    private final String allowedOrigins;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Value("${cors.allowed-origins}") String allowedOrigins,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.allowedOrigins = allowedOrigins;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    // DIP: Higher-level components depend on this SecurityFilterChain abstraction, not its implementation.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // SRP: Configures CSRF only, separate from other security responsibilities.

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // SRP: Delegates CORS configuration to a dedicated method → separation of concerns.

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/api/accounts/status").permitAll()
                                .requestMatchers("/actuator/**").permitAll()
                                .requestMatchers("/api/accounts/graphiql/**").permitAll()
                                .requestMatchers("/api/accounts/graphql").permitAll()
                                .anyRequest().authenticated()
                        // SRP: Only responsible for defining authorization rules (no business logic).
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // SRP: Dedicated method solely responsible for CORS configuration (no mixing with security rules).
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}
