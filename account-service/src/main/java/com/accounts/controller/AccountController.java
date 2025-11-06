package com.accounts.controller;

import com.accounts.dto.AccountRequest;
import com.accounts.dto.AccountTransactionRequest;
import com.accounts.entity.Account;
import com.accounts.service.impl.AccountServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountServiceImpl accountServiceImpl;

    @GetMapping("/status")
    public String getStatus() {
        log.info("GET /api/accounts/status");
        return "Account Service Running";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> createAccount(@RequestBody AccountRequest account) {
        log.info("POST /api/accounts/create with request: {}", account);
        Account createdAccount = accountServiceImpl.createAccount(account);
        log.info("Account created with id: {}", createdAccount.getAccountId());
        return ResponseEntity.ok(createdAccount);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCustomerOwner(#customerId)")
    public ResponseEntity<List<Account>> getAccountsByCustomer(@PathVariable String customerId) {
        log.info("Fetching accounts for customerId={}", customerId);
        List<Account> accounts = accountServiceImpl.getAccountsByCustomerId(customerId);
        log.info("Found {} accounts for customerId: {}", accounts.size(), customerId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        log.info("GET /api/accounts/{}", id);
        Account account = accountServiceImpl.getAccountById(id);
        log.info("Account found with id: {}", account.getAccountId());
        return ResponseEntity.ok(account);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        log.info("GET /api/accounts");
        List<Account> accounts = accountServiceImpl.getAllAccounts();
        log.info("Found {} accounts", accounts.size());
        return ResponseEntity.ok(accountServiceImpl.getAllAccounts());
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<String> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.info("POST /api/accounts/{}/deposit with amount: {}", id, amount);
        accountServiceImpl.deposit(id, amount);
        log.info("Deposit successful for accountId: {}", id);
        return ResponseEntity.ok("Deposit successful");
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<String> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.info("POST /api/accounts/{}/withdraw with amount: {}", id, amount);
        accountServiceImpl.withdraw(id, amount);
        log.info("Withdrawal successful for accountId: {}", id);
        return ResponseEntity.ok("Withdrawal successful");
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody AccountTransactionRequest request) {
        log.info("API Transfer request received: {}", request);
        Map<String, Object> response = new HashMap<>();
        try {
            accountServiceImpl.transfer(request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            response.put("status", "SUCCESS");
            response.put("message", "Transfer completed successfully");
            response.put("timestamp", LocalDateTime.now());
            log.info("Transfer from accountId: {} to accountId: {} successful", request.getFromAccountId(), request.getToAccountId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "FAILED");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            log.error("Transfer from accountId: {} to accountId: {} failed with error: {}", request.getFromAccountId(), request.getToAccountId(), e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
