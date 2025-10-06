package com.transaction.service;

import com.transaction.clients.AccountClient;
import com.transaction.dto.AccountTransactionRequest;
import com.transaction.dto.TransactionRequest;
import com.transaction.entity.Transaction;
import com.transaction.entity.TransactionStatus;
import com.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    /**
     * Perform a transaction: creates DB record, calls Account service with retries, and updates status.
     */
    public Transaction performTransaction(TransactionRequest request) {
        // Step 1: Create transaction record in DB
        Transaction txn = createTransactionRecord(request);

        // Step 2: Call Account service with retry logic
        try {
            callAccountServiceWithRetry(request);
            txn.setStatus(TransactionStatus.SUCCESS);
            log.info("Transaction SUCCESS: {}", txn.getReferenceNumber());
        } catch (Exception e) {
            log.error("Transaction FAILED for Account ID {}: {}", request.getAccountId(), e.getMessage(), e);
            txn.setStatus(TransactionStatus.FAILED);
            txn.setDescription("Account service failed: " + e.getMessage());
        }

        // Step 3: Update transaction status in DB
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
    private void callAccountServiceWithRetry(TransactionRequest request) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
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
                                request.getAmount()
                        ));
                    }
                }
                return; // success
            } catch (Exception e) {
                if (isLockTimeoutOrOptimisticLock(e)) {
                    attempt++;
                    log.warn("Lock timeout detected, retrying attempt {}/{}", attempt, maxRetries);
                    try {
                        Thread.sleep(100); // wait before retry
                    } catch (InterruptedException ignored) {}
                } else {
                    throw e; // fail immediately for other exceptions
                }
            }
        }

        throw new RuntimeException("Account service failed after " + maxRetries + " retries");
    }

    /**
     * Detect lock wait timeout or optimistic lock exceptions.
     */
    private boolean isLockTimeoutOrOptimisticLock(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return msg.contains("lock wait timeout") || msg.contains("optimistic lock");
    }
}
