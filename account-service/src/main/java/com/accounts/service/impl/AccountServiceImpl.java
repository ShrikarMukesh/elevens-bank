package com.accounts.service.impl;

import com.accounts.dto.AccountRequest;
import com.accounts.entity.Account;
import com.accounts.entity.AccountStatus;
import com.accounts.repository.AccountRepository;
import com.accounts.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(AccountRequest request) {
        Account account = Account.builder()
                .customerId(request.getCustomerId())
                .accountNumber(request.getAccountNumber())
                .accountType(request.getAccountType()) // Enum directly
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(AccountStatus.ACTIVE)
                .build();

        return accountRepository.save(account);
    }


    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Override
    public Account updateBalance(Long accountId, Double amount) {
        return null;
    }

    @Transactional
    public void deposit(Long accountId, BigDecimal amount) {
        log.info(">>> Deposit initiated for accountId={} amount={}", accountId, amount);

        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            BigDecimal oldBalance = account.getBalance();
            BigDecimal newBalance = oldBalance.add(amount);

            account.setBalance(newBalance);
            accountRepository.save(account);

            log.info(">>> Deposit successful. Old balance={}, New balance={}", oldBalance, newBalance);

        } catch (Exception e) {
            log.error("Deposit failed for accountId={} due to {}", accountId, e.getMessage(), e);
            throw e; // Important: rethrow so transaction rolls back
        }
    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new RuntimeException("Cannot transfer to same account");
        }

        // 🔹 Lock both accounts in a fixed order (deadlock prevention)
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("Target account not found"));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

}
