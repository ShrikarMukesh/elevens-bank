package com.cards.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            String roles = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.joining(", "));
            System.out.println("🔐 Authenticated Request → " + request.getMethod() + " " + request.getRequestURI());
            System.out.println("   👤 User: " + username);
            System.out.println("   🎭 Roles: " + roles);
        } else {
            System.out.println("🚫 Unauthenticated Request → " + request.getMethod() + " " + request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
