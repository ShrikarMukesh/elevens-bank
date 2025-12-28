package com.auth.config;

import com.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures the Security Filter Chain.
     *
     * <h2>Interview Topic: Spring Security Architecture</h2>
     * <p>
     * <b>Q: How does Spring Security work?</b><br>
     * A: It's a chain of Filters (DelegatingFilterProxy -> FilterChainProxy ->
     * SecurityFilterChain).
     * Requests pass through these filters before reaching the Servlet/Controller.
     * </p>
     * <p>
     * <b>Q: Why disable CSRF?</b><br>
     * A: CSRF (Cross-Site Request Forgery) attacks rely on browser cookies
     * (session-based auth).
     * Since we use JWT (Stateless Auth) stored in headers (Authorization: Bearer
     * ...), CSRF protection is unnecessary.
     * </p>
     * <p>
     * <b>Q: Why SessionCreationPolicy.STATELESS?</b><br>
     * A: We don't want the server to store a JSESSIONID. Each request must be
     * independently authenticated via JWT.
     * This makes the service scalable (RESTful).
     * </p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // ✅ Make /auth/** endpoints public
                        .requestMatchers("/auth/**").permitAll()
                        // ✅ Everything else needs authentication
                        .anyRequest().authenticated())
                // Interview Q: What happens if we don't set this?
                // A: Spring will create a JSESSIONID by default, treating it as a stateful app.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Interview Q: Why add filter BEFORE UsernamePasswordAuthenticationFilter?
                // A: We need to validate the JWT *before* Spring tries to perform standard
                // username/password login.
                // If JWT is valid, we set the SecurityContext, and Spring skips login.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
