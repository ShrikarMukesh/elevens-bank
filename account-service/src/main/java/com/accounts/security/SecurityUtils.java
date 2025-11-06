package com.accounts.security;

import com.accounts.entity.Account;
import com.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("securityUtils")
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final AccountRepository accountRepository;

    public static AuthPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
            log.debug("No AuthPrincipal found in SecurityContextHolder.");
            return null;
        }
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
        log.debug("Checking ownership for accountId: {} by principal: {}", accountId, getPrincipalUsername());
        AuthPrincipal p = getPrincipal();
        if (p == null || p.getCustomerId() == null) {
            log.warn("Ownership check failed: No authenticated principal or customer ID found.");
            return false;
        }

        Account acc = accountRepository.findById(accountId).orElse(null);
        if (acc == null) {
            log.warn("Ownership check failed: Account with ID {} not found.", accountId);
            return false;
        }

        boolean isOwner = acc.getCustomerId().equals(p.getCustomerId());
        log.debug("Account {} owner check result: {}. Account customerId: {}, Principal customerId: {}",
                  accountId, isOwner, acc.getCustomerId(), p.getCustomerId());
        return isOwner;
    }

    public static String getPrincipalUsername() {
        AuthPrincipal p = getPrincipal();
        return (p != null) ? p.getUsername() : "anonymous";
    }
}
