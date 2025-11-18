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

@Service // SRP: Marks this class as a service in the business layer (business logic only, no web concerns)
@Slf4j
public class AccountServiceImpl implements AccountService {
    // DIP: High-level code (controllers) depend on AccountService interface, not this concrete class directly
    // LSP: Any other implementation of AccountService can substitute this one without breaking callers
    // ISP: AccountService is a focused interface for account operations (not a "god" interface)

    private final AccountRepository accountRepository;
    // DIP: Depends on AccountRepository abstraction (Spring Data interface), not low-level DB code

    public AccountServiceImpl(AccountRepository accountRepository) {
        // DIP: Constructor injection – framework provides dependency, service doesn't construct it
        this.accountRepository = accountRepository;
    }

    public Account createAccount(AccountRequest request) {
        // SRP (method level): Only handles "create account" business logic
        log.info("Creating account for customerId={}", request.getCustomerId());
        try {
            Account account = Account.builder()
                    .customerId(request.getCustomerId())
                    .accountNumber(request.getAccountNumber())
                    .accountType(request.getAccountType()) // Enum directly
                    .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .status(AccountStatus.ACTIVE)
                    .build();

            Account savedAccount = accountRepository.save(account); // DIP: uses repository abstraction for persistence
            log.info("Account created successfully with accountId={}", savedAccount.getAccountId());
            return savedAccount;
        } catch (Exception e) {
            log.error("Account creation failed for customerId={} due to {}", request.getCustomerId(), e.getMessage(), e);
            throw e;
        }
    }

    public List<Account> getAccountsByCustomerId(String customerId) {
        // SRP: Only responsible for fetching accounts by customerId
        log.info("Fetching accounts for customerId={}", customerId);
        try {
            List<Account> accounts = accountRepository.findByCustomerId(customerId);
            log.info("Found {} accounts for customerId={}", accounts.size(), customerId);
            return accounts;
        } catch (Exception e) {
            log.error("Error fetching accounts for customerId={} due to {}", customerId, e.getMessage(), e);
            throw e;
        }
    }


    public Account getAccountById(Long accountId) {
        // SRP: Only responsible for fetching a single account by id
        log.info("Fetching account for accountId={}", accountId);
        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            log.info("Account found for accountId={}", accountId);
            return account;
        } catch (Exception e) {
            log.error("Error fetching account for accountId={} due to {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    public List<Account> getAllAccounts() {
        // SRP: Only responsible for fetching all accounts
        log.info("Fetching all accounts");
        try {
            List<Account> accounts = accountRepository.findAll();
            log.info("Found {} accounts", accounts.size());
            return accounts;
        } catch (Exception e) {
            log.error("Error fetching all accounts due to {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Account updateBalance(Long accountId, Double amount) {
        // SRP: Intended to handle balance update use case (currently not implemented)
        log.warn("updateBalance method is not implemented for accountId={}", accountId);
        return null;
    }

    @Transactional // SRP: Transactional boundary for this business operation only
    public void deposit(Long accountId, BigDecimal amount) {
        // SRP: Handles ONLY deposit rules (validation + balance update)
        log.info("Deposit initiated for accountId={} amount={}", accountId, amount);

        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            BigDecimal oldBalance = account.getBalance();
            BigDecimal newBalance = oldBalance.add(amount);

            account.setBalance(newBalance);
            accountRepository.save(account); // DIP: repository handles persistence details

            log.info("Deposit successful. Old balance={}, New balance={}", oldBalance, newBalance);

        } catch (Exception e) {
            log.error("Deposit failed for accountId={} due to {}", accountId, e.getMessage(), e);
            throw e; // SRP + transactional consistency: rethrow for rollback
        }
    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        // SRP: Handles ONLY withdrawal rules (insufficient funds, balance update)
        log.info("Withdraw initiated for accountId={} amount={}", accountId, amount);
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
            log.info("Withdraw successful. Old balance={}, New balance={}", oldBalance, newBalance);
        } catch (Exception e) {
            log.error("Withdraw failed for accountId={} due to {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // SRP: Encapsulates full transfer use case (validation + locking + updating two accounts)
        log.info("Transfer initiated from accountId={} to accountId={} for amount={}", fromAccountId, toAccountId, amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        try {
            // DIP: Repository provides locking method abstraction
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
            throw e; // SRP + transactional consistency: rollback on any failure
        }
    }

}
