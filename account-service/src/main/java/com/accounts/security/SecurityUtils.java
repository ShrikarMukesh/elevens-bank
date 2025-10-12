package com.accounts.security;

import com.accounts.entity.Account;
import com.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("securityUtils")
@RequiredArgsConstructor
public class SecurityUtils {

    private final AccountRepository accountRepository;

    public static AuthPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p) return p;
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

    public boolean isOwner(Long accountId) {
        AuthPrincipal p = getPrincipal();
        if (p == null || p.getCustomerId() == null) return false;
        Account acc = accountRepository.findById(accountId).orElse(null);
        return acc != null && acc.getCustomerId().equals(p.getCustomerId());
    }
}
