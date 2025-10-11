package com.accounts.security;

import java.util.List;

public class AuthPrincipal {
    private final String username;
    private final Long customerId;         // may be null for staff/admin
    private final List<String> roles;

    public AuthPrincipal(String username, Long customerId, List<String> roles) {
        this.username = username;
        this.customerId = customerId;
        this.roles = roles;
    }

    public String getUsername() { return username; }
    public Long getCustomerId() { return customerId; }
    public List<String> getRoles() { return roles; }

    public boolean hasRole(String r) {
        return roles != null && roles.stream().anyMatch(rr -> rr.equalsIgnoreCase(r));
    }
}
