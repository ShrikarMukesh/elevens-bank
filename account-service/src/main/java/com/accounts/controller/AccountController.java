package com.accounts.controller;

import com.accounts.dto.AccountRequest;
import com.accounts.entity.Account;
import com.accounts.service.impl.AccountServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountServiceImpl accountServiceImpl;

    public AccountController(AccountServiceImpl accountServiceImpl) {
        this.accountServiceImpl = accountServiceImpl;
    }

    @GetMapping("/status")
    public String getStatus(){
        return "Account Service Running";
    }

    @PostMapping("/create")
    public Account createAccount( @RequestBody AccountRequest account) {
        return accountServiceImpl.createAccount(account);
    }

    @GetMapping("/{id}")
    public Account getAccount(@PathVariable Long id) {
        return accountServiceImpl.getAccountById(id);
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountServiceImpl.getAllAccounts();
    }

    @PutMapping("/{id}/balance")
    public Account updateBalance(@PathVariable Long id, @RequestParam Double amount) {
        return accountServiceImpl.updateBalance(id, amount);
    }


    @PostMapping("/{id}/deposit")
    public ResponseEntity<String> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        accountServiceImpl.deposit(id, amount);
        return ResponseEntity.ok("Deposit successful");
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<String> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        accountServiceImpl.withdraw(id, amount);
        return ResponseEntity.ok("Withdrawal successful");
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam BigDecimal amount) {
        accountServiceImpl.transfer(fromAccountId, toAccountId, amount);
        return ResponseEntity.ok("Transfer successful");
    }
}
