package com.accounts.service;

import com.accounts.dto.AccountRequest;
import com.accounts.entity.Account;

import java.math.BigDecimal;
import java.util.List;

/*
✅ **S — Single Responsibility Principle**
✅ **O — Open/Closed Principle**
✅ **L — Liskov Substitution Principle**
✅ **I — Interface Segregation Principle**
✅ **D — Dependency Inversion Principle**
 */

public interface AccountService {
    // SRP: This interface has a single responsibility – define the contract for account-related operations.
    // DIP: Higher-level layers (controllers) depend on this abstraction instead of concrete implementations.
    // LSP: Any implementation of AccountService (e.g., AccountServiceImpl, MockAccountService) should be swappable without breaking callers.
    // ISP: Focused on account domain only – not mixing unrelated responsibilities like customer, loans, etc.

    Account createAccount(AccountRequest account);
    // SRP: Contract only for account creation use case.

    Account getAccountById(Long accountId);
    // SRP: Contract for retrieving a single account.

    List<Account> getAccountsByCustomerId(String customerId);
    // SRP: Contract for retrieving accounts by customer ID.

    List<Account> getAllAccounts();
    // SRP: Contract for retrieving all accounts.

    Account updateBalance(Long accountId, Double amount);
    // SRP: Contract to update balance (business rule can vary per implementation).

    void deposit(Long id, BigDecimal amount);
    // SRP: Contract for deposit operation.

    public void withdraw(Long accountId, BigDecimal amount);
    // SRP: Contract for withdrawal use case.

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount);
    // SRP: Contract for money transfer between accounts.
}
