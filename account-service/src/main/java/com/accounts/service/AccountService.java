package com.accounts.service;

import com.accounts.dto.AccountRequest;
import com.accounts.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
     Account createAccount(AccountRequest account);
     Account getAccountById(Long accountId);
     List<Account> getAllAccounts();
     Account updateBalance(Long accountId, Double amount);

    void deposit(Long id, BigDecimal amount);

    public void withdraw(Long accountId, BigDecimal amount);

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount);
}
