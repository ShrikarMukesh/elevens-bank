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
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountServiceImpl accountServiceImpl;

    @GetMapping("/status")
    public String getStatus() { return "Account Service Running"; }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> createAccount(@RequestBody AccountRequest account) {
        return ResponseEntity.ok(accountServiceImpl.createAccount(account));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCustomerOwner(#customerId)")
    public ResponseEntity<List<Account>> getAccountsByCustomer(@PathVariable String customerId) {
        List<Account> accounts = accountServiceImpl.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountServiceImpl.getAccountById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountServiceImpl.getAllAccounts());
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<String> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        accountServiceImpl.deposit(id, amount);
        return ResponseEntity.ok("Deposit successful");
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#id)")
    public ResponseEntity<String> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        accountServiceImpl.withdraw(id, amount);
        return ResponseEntity.ok("Withdrawal successful");
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwner(#request.fromAccountId)")
    public ResponseEntity<String> transfer(@RequestBody AccountTransactionRequest request) {
        accountServiceImpl.transfer(request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        return ResponseEntity.ok("Transfer successful");
    }
}
