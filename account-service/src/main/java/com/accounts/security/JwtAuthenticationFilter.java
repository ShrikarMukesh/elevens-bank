package com.accounts.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor // DIP + IoC: Spring injects JwtUtil instead of this class creating it.
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    // DIP: Depends on JwtUtil abstraction for token operations instead of implementing token logic here.
    // SRP: This filter is only responsible for authentication, not token creation or authorization.

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String header = request.getHeader("Authorization");

        // SRP: This block handles ONLY the responsibility of checking for token existence.
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // SRP: This block handles ONLY JWT validation and authentication context setup.
        try {
            final String token = header.substring(7);

            // SRP: Validation delegated to JwtUtil (separation of concerns).
            if (jwtUtil.validate(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                // LSP: UsernamePasswordAuthenticationToken follows LSP—any subclass of Authentication works in the security context.
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SRP: Only sets authentication; does NOT handle authorization or roles.
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.info("Authenticated user '{}' with role '{}'", username, role);
            }
        } catch (Exception e) {
            // SRP: Only logs token-related exceptions, does not handle unrelated errors.
            log.warn("Invalid JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext(); // SRP: clear only the security context responsibility.
        }

        // SRP: Always continues the filter chain. No business logic or login logic here.
        chain.doFilter(request, response);
    }
}
