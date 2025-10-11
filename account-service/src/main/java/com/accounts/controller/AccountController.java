package com.accounts.controller;

import com.accounts.dto.AccountRequest;
import com.accounts.dto.AccountTransactionRequest;
import com.accounts.entity.Account;
import com.accounts.security.SecurityUtils;
import com.accounts.service.impl.AccountServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// imports omitted for brevity
@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class AccountController {

    private final AccountServiceImpl accountServiceImpl;

    public AccountController(AccountServiceImpl accountServiceImpl) {
        this.accountServiceImpl = accountServiceImpl;
    }

    @GetMapping("/status")
    public String getStatus() { return "Account Service Running"; }

    // ADMIN only (create account)
    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(@RequestBody AccountRequest account) {
        if (!SecurityUtils.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Account created = accountServiceImpl.createAccount(account);
        return ResponseEntity.ok(created);
    }

    // Get single account — owner or admin or staff (depending on policy)
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        Account acc = accountServiceImpl.getAccountById(id);

        Long tokenCustomerId = SecurityUtils.getCustomerId();
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isAdmin) {
            // customers can only access their own accounts
            if (tokenCustomerId == null || !tokenCustomerId.equals(acc.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(acc);
    }

    // List all accounts — admin only
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        if (!SecurityUtils.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(accountServiceImpl.getAllAccounts());
    }

    // Update balance — admin only
    @PutMapping("/{id}/balance")
    public ResponseEntity<Account> updateBalance(@PathVariable Long id, @RequestParam Double amount) {
        if (!SecurityUtils.isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Account updated = accountServiceImpl.updateBalance(id, amount);
        return ResponseEntity.ok(updated);
    }

    // Deposit — owner or admin
    @PostMapping("/{id}/deposit")
    public ResponseEntity<String> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Account acc = accountServiceImpl.getAccountById(id);
        Long tokenCustomerId = SecurityUtils.getCustomerId();
        if (!SecurityUtils.isAdmin()) {
            if (tokenCustomerId == null || !tokenCustomerId.equals(acc.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not owner of account");
            }
        }

        accountServiceImpl.deposit(id, amount);
        return ResponseEntity.ok("Deposit successful");
    }

    // Withdraw — owner or admin
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<String> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Account acc = accountServiceImpl.getAccountById(id);
        Long tokenCustomerId = SecurityUtils.getCustomerId();
        if (!SecurityUtils.isAdmin()) {
            if (tokenCustomerId == null || !tokenCustomerId.equals(acc.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not owner of account");
            }
        }
        accountServiceImpl.withdraw(id, amount);
        return ResponseEntity.ok("Withdrawal successful");
    }

    // Transfer — require owner of source account (or admin)
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody AccountTransactionRequest request) {
        Account from = accountServiceImpl.getAccountById(request.getFromAccountId());
        Long tokenCustomerId = SecurityUtils.getCustomerId();
        if (!SecurityUtils.isAdmin()) {
            if (tokenCustomerId == null || !tokenCustomerId.equals(from.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not owner of source account");
            }
        }

        accountServiceImpl.transfer(request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        return ResponseEntity.ok("Transfer successful");
    }
}

