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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * Service Implementation for Account Operations.
 *
 * <h2>Interview Topic: Service Layer Responsibility</h2>
 * <p>
 * <b>Q: What is the role of the Service Layer?</b><br>
 * A: It encapsulates business logic, transaction management, and coordinates
 * between Repositories and other services.
 * It enforces the "Single Responsibility Principle" (SRP) by keeping business
 * rules out of the Controller.
 * </p>
 */
@Service // SRP: Marks this class as a service in the business layer (business logic
         // only, no web concerns)
@Slf4j
public class AccountServiceImpl implements AccountService {
    // DIP: High-level code (controllers) depend on AccountService interface, not
    // this concrete class directly
    // LSP: Any other implementation of AccountService can substitute this one
    // without breaking callers
    // ISP: AccountService is a focused interface for account operations (not a
    // "god" interface)

    private final AccountRepository accountRepository;
    // DIP: Depends on AccountRepository abstraction (Spring Data interface), not
    // low-level DB code

    public AccountServiceImpl(AccountRepository accountRepository) {
        // DIP: Constructor injection – framework provides dependency, service doesn't
        // construct it
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
            log.error("Account creation failed for customerId={} due to {}", request.getCustomerId(), e.getMessage(),
                    e);
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

    // @Cacheable: Caches the result of this method.
    // value = "accounts": The name of the cache container (e.g., a Redis hash or
    // Map).
    // key = "#accountId": The key for the cache entry. Spring Expression Language
    // (SpEL) uses the method argument 'accountId'.
    // If the data exists in the cache, the method is skipped, and the cached value
    // is returned.
    // If not, the method executes, and the result is stored in the cache.
    /**
     * Retrieves an account by ID.
     *
     * <h2>Interview Topic: Caching Strategy</h2>
     * <p>
     * <b>Q: Why use @Cacheable here?</b><br>
     * A: Account details don't change often but are read frequently. Caching
     * reduces Database load (IOPS)
     * and latency. We use the "Look-Aside" pattern (lazy loading): check cache, if
     * miss, check DB and populate cache.
     * </p>
     * <p>
     * <b>Q: What happens if the data changes?</b><br>
     * A: We must evict or update the cache. See {@code deposit()} or
     * {@code withdraw()} for @CacheEvict usage.
     * This ensures "Eventual Consistency".
     * </p>
     */
    @Cacheable(value = "accounts", key = "#accountId")
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

    /**
     * Deposits money into an account.
     *
     * <h2>Interview Topic: Transaction Management (ACID)</h2>
     * <p>
     * <b>Q: What does @Transactional do?</b><br>
     * A: It ensures Atomicity. If any line fails (throws RuntimeException), the
     * entire operation rolls back.
     * Database commits happen only if the method completes successfully.
     * </p>
     * <p>
     * <b>Q: Why @CacheEvict?</b><br>
     * A: Cache Invalidation. Since we changed the balance, the old cache entry is
     * stale (dirty read risk).
     * Removing it forces the next read to fetch fresh data from the DB.
     * </p>
     */
    @Transactional // SRP: Transactional boundary for this business operation only
    // @CacheEvict: Removes an entry from the cache.
    // value = "accounts": The cache container to modify.
    // key = "#accountId": The specific key to remove.
    // This ensures that when an account is updated (deposit), the stale data in the
    // cache is deleted.
    // The next read (getAccountById) will fetch fresh data from the DB and re-cache
    // it.
    @CacheEvict(value = "accounts", key = "#accountId")
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
    // @CacheEvict: Removes the cache entry for this account.
    // Similar to deposit, this ensures that after a withdrawal, the cache doesn't
    // hold the old balance.
    // This maintains data consistency between the database and the cache.
    @CacheEvict(value = "accounts", key = "#accountId")
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

    /**
     * Transfers money between two accounts.
     *
     * <h2>Interview Topic: Concurrency & Deadlocks</h2>
     * <p>
     * <b>Q: How do you handle concurrent transfers?</b><br>
     * A: We use <b>Pessimistic Locking</b> ({@code select for update}) via
     * {@code findByIdForUpdate}.
     * This prevents two threads from modifying the same account simultaneously
     * (Lost Update problem).
     * </p>
     * <p>
     * <b>Q: How do you prevent Deadlocks?</b><br>
     * A: Deadlock happens if Thread A locks Acc1 then wants Acc2, while Thread B
     * locks Acc2 then wants Acc1.
     * <b>Solution:</b> Always acquire locks in a consistent order (e.g., by Account
     * ID).
     * <br>
     * {@code Long firstLockId = fromAccountId < toAccountId ? fromAccountId : toAccountId;}
     * </p>
     */
    @Transactional
    // @Caching: Allows multiple cache operations to be applied to a single method.
    // Since a transfer affects two accounts (sender and receiver), we need to evict
    // both from the cache.
    // @CacheEvict(key = "#fromAccountId"): Removes the sender's account data from
    // the cache.
    // @CacheEvict(key = "#toAccountId"): Removes the receiver's account data from
    // the cache.
    // This ensures both accounts will be re-fetched from the DB with updated
    // balances on the next read.
    @Caching(evict = {
            @CacheEvict(value = "accounts", key = "#fromAccountId"),
            @CacheEvict(value = "accounts", key = "#toAccountId")
    })
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // SRP: Encapsulates full transfer use case (validation + locking + updating two
        // accounts)
        log.info("Transfer initiated from accountId={} to accountId={} for amount={}", fromAccountId, toAccountId,
                amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        try {
            // DIP: Repository provides locking method abstraction
            // Prevent Deadlock: Always lock accounts in consistent order (smaller ID first)
            Long firstLockId = fromAccountId < toAccountId ? fromAccountId : toAccountId;
            Long secondLockId = fromAccountId < toAccountId ? toAccountId : fromAccountId;

            Account firstAccount = accountRepository.findByIdForUpdate(firstLockId)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + firstLockId));
            Account secondAccount = accountRepository.findByIdForUpdate(secondLockId)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + secondLockId));

            Account fromAccount = firstAccount.getAccountId().equals(fromAccountId) ? firstAccount : secondAccount;
            Account toAccount = firstAccount.getAccountId().equals(toAccountId) ? firstAccount : secondAccount;

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
