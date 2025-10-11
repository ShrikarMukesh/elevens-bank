package com.accounts.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static AuthPrincipal getPrincipal() {
        Authentication auth = getAuthentication();
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof AuthPrincipal) return (AuthPrincipal) p;
        return null;
    }

    public static boolean isAdmin() {
        AuthPrincipal p = getPrincipal();
        return p != null && p.hasRole("ADMIN");
    }

    public static Long getCustomerId() {
        AuthPrincipal p = getPrincipal();
        return (p != null) ? p.getCustomerId() : null;
    }

    public static String getUsername() {
        AuthPrincipal p = getPrincipal();
        return (p != null) ? p.getUsername() : null;
    }
}
