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

/*
✅ **S — Single Responsibility Principle**
✅ **O — Open/Closed Principle**
✅ **L — Liskov Substitution Principle**
✅ **I — Interface Segregation Principle**
✅ **D — Dependency Inversion Principle**
 */

@RestController // SRP: This class is responsible only for handling HTTP requests/responses for accounts.
@RequestMapping("/api/accounts")
@Slf4j
@RequiredArgsConstructor // DIP/IoC: Dependencies are injected via constructor (generated), controller doesn't create them.
public class AccountController {

    private final AccountServiceImpl accountServiceImpl;
    // DIP (partially): Depends on a separate service layer instead of doing business logic here.
    // (Would be stronger DIP if this was AccountService interface.)

    @GetMapping("/status")
    public String getStatus() {
        // SRP (method level): Only returns service health/status info.
        log.info("GET /api/accounts/status");
        return "Account Service Running";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    // SRP/SoC: Security concerns are handled declaratively via annotation, not hardcoded in method logic.
    public ResponseEntity<Account> createAccount(@RequestBody AccountRequest account) {
        // SRP: Method handles HTTP-level concerns (logging, request/response) and delegates business logic to service.
        log.info("POST /api/accounts/create with request: {}", account);
        Account createdAccount = accountServiceImpl.createAccount(account); // SRP: business logic in service layer.
        log.info("Account created with id: {}", createdAccount.getAccountId());
        return ResponseEntity.ok(createdAccount); // OCP-friendly: response building is localized here.
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCustomerOwner(#customerId)")
    // SRP/SoC: Authorization is separated from core method logic.
    public ResponseEntity<List<Account>> getAccountsByCustomer(@PathVariable String customerId) {
        // SRP: Method only coordinates request → service → response flow.
        log.info("Fetching accounts for customerId={}", customerId);
        List<Account> accounts = accountServiceImpl.getAccountsByCustomerId(customerId); // SRP: delegate to service.
        log.info("Found {} accounts for customerId: {}", accounts.size(), customerId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        // SRP: Only responsible for HTTP mapping and response, not fetching logic itself.
        log.info("GET /api/accounts/{}", id);
        Account account = accountServiceImpl.getAccountById(id); // SRP: delegation to service.
        log.info("Account found with id: {}", account.getAccountId());
        return ResponseEntity.ok(account);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        // SRP: Dedicated to "get all accounts" API behavior.
        log.info("GET /api/accounts");
        List<Account> accounts = accountServiceImpl.getAllAccounts(); // SRP: business logic in service.
        log.info("Found {} accounts", accounts.size());
        return ResponseEntity.ok(accountServiceImpl.getAllAccounts());
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<String> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        // SRP: Handles only HTTP interaction for deposit; actual deposit rules are in service.
        log.info("POST /api/accounts/{}/deposit with amount: {}", id, amount);
        accountServiceImpl.deposit(id, amount); // SRP: delegate to service for business logic.
        log.info("Deposit successful for accountId: {}", id);
        return ResponseEntity.ok("Deposit successful");
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<String> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        // SRP: Only orchestrates the withdraw HTTP API.
        log.info("POST /api/accounts/{}/withdraw with amount: {}", id, amount);
        accountServiceImpl.withdraw(id, amount); // SRP: actual logic in service.
        log.info("Withdrawal successful for accountId: {}", id);
        return ResponseEntity.ok("Withdrawal successful");
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody AccountTransactionRequest request) {
        // SRP: This method is responsible for building the HTTP-level transfer response.
        log.info("API Transfer request received: {}", request);
        Map<String, Object> response = new HashMap<>();
        try {
            accountServiceImpl.transfer(request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            // SRP: Transfer business logic is fully delegated to service.
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
