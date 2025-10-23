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
        log.info(">>> Creating account for customerId={}", request.getCustomerId());
        try {
            Account account = Account.builder()
                    .customerId(request.getCustomerId())
                    .accountNumber(request.getAccountNumber())
                    .accountType(request.getAccountType()) // Enum directly
                    .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .status(AccountStatus.ACTIVE)
                    .build();

            Account savedAccount = accountRepository.save(account);
            log.info(">>> Account created successfully with accountId={}", savedAccount.getAccountId());
            return savedAccount;
        } catch (Exception e) {
            log.error(">>> Account creation failed for customerId={} due to {}", request.getCustomerId(), e.getMessage(), e);
            throw e;
        }
    }

    public List<Account> getAccountsByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }


    public Account getAccountById(Long accountId) {
        log.info(">>> Fetching account for accountId={}", accountId);
        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            log.info(">>> Account found for accountId={}", accountId);
            return account;
        } catch (Exception e) {
            log.error(">>> Error fetching account for accountId={} due to {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    public List<Account> getAllAccounts() {
        log.info(">>> Fetching all accounts");
        try {
            List<Account> accounts = accountRepository.findAll();
            log.info(">>> Found {} accounts", accounts.size());
            return accounts;
        } catch (Exception e) {
            log.error(">>> Error fetching all accounts due to {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Account updateBalance(Long accountId, Double amount) {
        log.warn("updateBalance method is not implemented for accountId={}", accountId);
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
        log.info(">>> Withdraw initiated for accountId={} amount={}", accountId, amount);
        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            if (account.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }

            BigDecimal oldBalance = account.getBalance();
            BigDecimal newBalance = oldBalance.subtract(amount);

            account.setBalance(newBalance);
            accountRepository.save(account);
            log.info(">>> Withdraw successful. Old balance={}, New balance={}", oldBalance, newBalance);
        } catch (Exception e) {
            log.error("Withdraw failed for accountId={} due to {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        log.info(">>> Transfer initiated from accountId={} to accountId={} for amount={}", fromAccountId, toAccountId, amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        try {
            // Lock both accounts
            Account fromAccount = accountRepository.findByIdForUpdate(fromAccountId)
                    .orElseThrow(() -> new RuntimeException("Source account not found"));
            Account toAccount = accountRepository.findByIdForUpdate(toAccountId)
                    .orElseThrow(() -> new RuntimeException("Target account not found"));

            if (fromAccount.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }

            BigDecimal fromNewBalance = fromAccount.getBalance().subtract(amount);
            BigDecimal toNewBalance = toAccount.getBalance().add(amount);

            fromAccount.setBalance(fromNewBalance);
            toAccount.setBalance(toNewBalance);

            accountRepository.saveAndFlush(fromAccount);
            accountRepository.saveAndFlush(toAccount);

            log.info("✅ Transfer successful. From A/C {} → ₹{} → To A/C {}",
                    fromAccount.getAccountNumber(), amount, toAccount.getAccountNumber());
            log.info("Updated Balances: fromAccount={}, toAccount={}", fromNewBalance, toNewBalance);

        } catch (Exception e) {
            log.error("❌ Transfer failed from accountId={} to accountId={} for amount={} due to {}",
                    fromAccountId, toAccountId, amount, e.getMessage(), e);
            throw e; // rollback on any failure
        }
    }

}
