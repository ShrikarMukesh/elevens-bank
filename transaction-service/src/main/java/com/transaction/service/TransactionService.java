package com.transaction.service;

import com.transaction.clients.AccountClient;
import com.transaction.clients.CustomerClient;
import com.transaction.dto.AccountResponse;
import com.transaction.dto.AccountTransactionRequest;
import com.transaction.dto.CustomerResponse;
import com.transaction.dto.TransactionRequest;
import com.transaction.entity.Transaction;
import com.transaction.entity.TransactionStatus;
import com.transaction.exception.DownstreamServiceException;
import com.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    @Lazy
    private TransactionService self;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
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
     *
     * <h2>Interview Topic: Distributed Transactions</h2>
     * <p>
     * <b>Q: How do you handle transactions across microservices?</b><br>
     * A: This method implements the <b>Saga Pattern (Orchestration)</b>.
     * <ol>
     * <li><b>Local Tx:</b> Create PENDING transaction record.</li>
     * <li><b>Remote Call:</b> Call Account Service to update balance.</li>
     * <li><b>Local Tx:</b> Update status to SUCCESS or FAILED.</li>
     * </ol>
     * <br>
     * <b>Q: What is the "Dual Write Problem" here?</b><br>
     * A: If the server crashes <i>after</i> the Account Service call but
     * <i>before</i> updating status to SUCCESS,
     * the system ends up in an inconsistent state (Money moved, but Transaction
     * says PENDING).
     * <b>Solution:</b> Use an Outbox Pattern or reconcile PENDING transactions via
     * a background scheduler.
     * </p>
     */
    public Transaction performTransaction(TransactionRequest request) {
        // Step 1: Create PENDING transaction in a new transaction (REQUIRES_NEW).
        // This ensures the record exists even if the subsequent logic fails.
        Transaction txn = self.createTransactionRecord(request);

        try {
            // Step 2: Call external service (Business Logic)
            self.callAccountServiceWithRetry(request); // may throw typed Downstream* exceptions

            // Step 3: Update status to SUCCESS
            txn.setStatus(TransactionStatus.SUCCESS);
            log.info("Transaction SUCCESS: {}", txn.getReferenceNumber());

            // Step 4: Publish event to Kafka for Notification Service
            publishTransactionEvent(txn);

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
     * Uses REQUIRES_NEW to ensure this record is committed immediately,
     * acting as an audit trail even if the main flow fails later.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction createTransactionRecord(TransactionRequest request) {
        Transaction txn = Transaction.builder()
                .accountId(request.accountId())
                .transactionType(request.transactionType())
                .amount(request.amount())
                .transactionMode(request.transactionMode()) // ✅ Updated to use transactionMode
                .referenceNumber(UUID.randomUUID().toString())
                .status(TransactionStatus.PENDING)
                .description(request.description())
                .build();

        txn = transactionRepository.save(txn);
        log.debug("Transaction record created with ID: {}", txn.getTransactionId());
        return txn;
    }

    /**
     * Call Account service with retry logic for lock wait/optimistic exceptions.
     *
     * <h2>Interview Topic: Resiliency & Retries</h2>
     * <p>
     * <b>Q: Why use @Retryable?</b><br>
     * A: Network calls or Database locks are transient failures. Retrying
     * immediately often fixes the issue.
     * <br>
     * <b>Q: What is "Exponential Backoff"?</b><br>
     * A: The {@code @Backoff} annotation introduces a delay between retries.
     * Ideally, this delay should increase (100ms, 200ms, 400ms) to avoid hammering
     * a struggling downstream service ("Thundering Herd" problem).
     * </p>
     */
    @Retryable(value = { RetryableException.class,
            feign.RetryableException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void callAccountServiceWithRetry(TransactionRequest request) {
        try {
            switch (request.transactionType()) {
                case DEPOSIT -> {
                    log.info("Performing DEPOSIT for account: {}", request.accountId());
                    accountClient.deposit(request.accountId(), request.amount());
                }
                case WITHDRAWAL -> {
                    log.info("Performing WITHDRAWAL for account: {}", request.accountId());
                    accountClient.withdraw(request.accountId(), request.amount());
                }
                case TRANSFER -> {
                    log.info("Performing TRANSFER from account {} to {}",
                            request.accountId(), request.targetAccountId());
                    accountClient.transfer(new AccountTransactionRequest(
                            request.accountId(),
                            request.targetAccountId(),
                            request.amount()));
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

    private void publishTransactionEvent(Transaction txn) {
        try {
            // Fetch account details to get customerId
            AccountResponse account = accountClient.getAccount(txn.getAccountId());
            String customerId = account.customerId();
            
            // Fetch customer details to get name and email
            CustomerResponse customer = customerClient.getCustomer(customerId);
            String customerName = customer.firstName() + " " + customer.lastName();
            String customerEmail = customer.email();

            Map<String, Object> event = new HashMap<>();
            event.put("eventSource", "TRANSACTION_SERVICE");

            String eventType = switch (txn.getTransactionType()) {
                case DEPOSIT -> "CREDIT_ALERT";
                case WITHDRAWAL -> "DEBIT_ALERT";
                case TRANSFER -> "DEBIT_ALERT";
                default -> "TRANSACTION_ALERT";
            };

            event.put("eventType", eventType);
            event.put("customerId", customerId);
            event.put("accountId", txn.getAccountId().toString());
            event.put("channel", "EMAIL");
            event.put("email", customerEmail); // Add email for notification service
            event.put("eventTime", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("amount", txn.getAmount());

            String maskedAccount = "XXXX" + txn.getAccountId().toString()
                    .substring(Math.max(0, txn.getAccountId().toString().length() - 4));
            data.put("maskedAccount", maskedAccount);

            data.put("mode", txn.getTransactionMode());
            data.put("referenceId", txn.getReferenceNumber());
            data.put("transactionDate", java.time.LocalDate.now().toString());
            data.put("customerName", customerName);

            // Customize source based on transaction type for better messages
            if (txn.getTransactionType() == com.transaction.entity.TransactionType.DEPOSIT) {
                data.put("source", "Cash Deposit/Transfer");
            } else if (txn.getTransactionType() == com.transaction.entity.TransactionType.TRANSFER) {
                data.put("source", "Transfer to other account");
            } else {
                data.put("source", "ATM/Bank Withdrawal");
            }

            event.put("data", data);

            kafkaTemplate.send("bank.events", event);
            log.info("Published transaction event to Kafka: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to publish transaction event", e);
        }
    }

    private static class RetryableException extends RuntimeException {
        public RetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
