package com.transaction.service;

import com.transaction.clients.AccountClient;
import com.transaction.dto.AccountTransactionRequest;
import com.transaction.dto.TransactionRequest;
import com.transaction.entity.Transaction;
import com.transaction.entity.TransactionStatus;
import com.transaction.exception.DownstreamServiceException;
import com.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    @Autowired
    @Lazy
    private TransactionService self;

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        log.info("Fetching transactions for accountId={}", accountId);
        try {
            List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
            log.info("Found {} transactions for accountId={}", transactions.size(), accountId);
            return transactions;
        } catch (Exception e) {
            log.error("Error fetching transactions for accountId={} due to {}", accountId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Perform a transaction: creates DB record, calls Account service with retries,
     * and updates status.
     */
    public Transaction performTransaction(TransactionRequest request) {
        Transaction txn = createTransactionRecord(request);
        try {
            self.callAccountServiceWithRetry(request); // may throw typed Downstream* exceptions
            txn.setStatus(TransactionStatus.SUCCESS);
            log.info("Transaction SUCCESS: {}", txn.getReferenceNumber());
        } catch (DownstreamServiceException ex) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setDescription("Downstream %s failed: %s".formatted(ex.getService(), ex.getMessage()));
            transactionRepository.save(txn);
            throw ex; // let @ControllerAdvice map to proper HTTP (502/403/etc.)
        } catch (Exception e) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setDescription("Unexpected error: " + e.getMessage());
            transactionRepository.save(txn);
            throw e;
        }
        return transactionRepository.save(txn);
    }

    /**
     * Create initial transaction record in PENDING state.
     */
    @Transactional
    public Transaction createTransactionRecord(TransactionRequest request) {
        Transaction txn = Transaction.builder()
                .accountId(request.getAccountId())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .modeId(request.getModeId())
                .referenceNumber(UUID.randomUUID().toString())
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        txn = transactionRepository.save(txn);
        log.debug("Transaction record created with ID: {}", txn.getTransactionId());
        return txn;
    }

    /**
     * Call Account service with retry logic for lock wait/optimistic exceptions.
     */
    @Retryable(value = RetryableException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void callAccountServiceWithRetry(TransactionRequest request) {
        try {
            switch (request.getTransactionType()) {
                case DEPOSIT -> {
                    log.info("Performing DEPOSIT for account: {}", request.getAccountId());
                    accountClient.deposit(request.getAccountId(), request.getAmount());
                }
                case WITHDRAWAL -> {
                    log.info("Performing WITHDRAWAL for account: {}", request.getAccountId());
                    accountClient.withdraw(request.getAccountId(), request.getAmount());
                }
                case TRANSFER -> {
                    log.info("Performing TRANSFER from account {} to {}",
                            request.getAccountId(), request.getTargetAccountId());
                    accountClient.transfer(new AccountTransactionRequest(
                            request.getAccountId(),
                            request.getTargetAccountId(),
                            request.getAmount()));
                }
            }
        } catch (Exception e) {
            if (isLockTimeoutOrOptimisticLock(e)) {
                log.warn("Lock timeout detected, retrying...");
                throw new RetryableException("Retryable error", e);
            }
            throw e; // fail immediately for other exceptions
        }
    }

    /**
     * Detect lock wait timeout or optimistic lock exceptions.
     */
    private boolean isLockTimeoutOrOptimisticLock(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return msg.contains("lock wait timeout") || msg.contains("optimistic lock");
    }

    private static class RetryableException extends RuntimeException {
        public RetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
