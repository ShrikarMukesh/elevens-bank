package com.elevens.loanservice.service;

import com.elevens.loanservice.entity.AuditLog;
import com.elevens.loanservice.exception.ResourceNotFoundException;
import com.elevens.loanservice.model.Loan;
import com.elevens.loanservice.model.LoanStatus;
import com.elevens.loanservice.repository.AuditLogRepository;
import com.elevens.loanservice.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final AuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Loan applyLoan(String customerId, BigDecimal amount, BigDecimal interestRate, Integer tenureMonths) {
        log.info("Processing loan application for customerId: {}", customerId);

        Loan loan = Loan.builder()
                .customerId(customerId)
                .amount(amount)
                .interestRate(interestRate)
                .tenureMonths(tenureMonths)
                .status(LoanStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();

        Loan savedLoan = loanRepository.save(loan);

        // Audit Log: LOAN_APPLIED
        saveAuditLog("LOAN_APPLIED", customerId, "LOAN", String.valueOf(savedLoan.getId()),
                "Loan application submitted for amount: " + amount, 201, null);

        log.info("Loan application created with id: {}", savedLoan.getId());
        return savedLoan;
    }

    public List<Loan> getLoansByCustomer(String customerId) {
        log.info("Fetching loans for customerId: {}", customerId);
        return loanRepository.findByCustomerId(customerId);
    }

    public Loan approveLoan(Long loanId) {
        log.info("Approving loan with id: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());

        Loan savedLoan = loanRepository.save(loan);

        // Audit Log: LOAN_APPROVED
        saveAuditLog("LOAN_APPROVED", loan.getCustomerId(), "LOAN", String.valueOf(loanId),
                "Loan approved for amount: " + loan.getAmount(), 200, null);

        // Publish event to Kafka
        publishLoanEvent(savedLoan, "LOAN_DISBURSED");

        log.info("Loan approved with id: {}", loanId);
        return savedLoan;
    }

    public Loan rejectLoan(Long loanId, String reason) {
        log.info("Rejecting loan with id: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        loan.setStatus(LoanStatus.REJECTED);

        Loan savedLoan = loanRepository.save(loan);

        // Audit Log: LOAN_REJECTED
        saveAuditLog("LOAN_REJECTED", loan.getCustomerId(), "LOAN", String.valueOf(loanId),
                "Loan rejected. Reason: " + reason, 200, null);

        log.info("Loan rejected with id: {}", loanId);
        return savedLoan;
    }

    private void publishLoanEvent(Loan loan, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventSource", "LOAN_SERVICE");
            event.put("eventType", eventType);
            event.put("customerId", loan.getCustomerId());
            event.put("accountId", "LOAN-" + loan.getId()); // Placeholder for loan account
            event.put("channel", "EMAIL");
            event.put("eventTime", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("amount", loan.getAmount());
            data.put("loanId", loan.getId());
            data.put("accountNumber", "XXXX" + loan.getId()); // Placeholder
            data.put("customerName", "Customer"); // Placeholder

            event.put("data", data);

            kafkaTemplate.send("bank.events", event);
            log.info("Published loan event to Kafka: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to publish loan event", e);
        }
    }

    /**
     * Saves an audit log entry for loan events.
     * Wrapped in try-catch to prevent audit failures from affecting business logic.
     */
    private void saveAuditLog(String eventType, String userId, String entityType, String entityId,
            String description, Integer statusCode, String errorMessage) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .serviceName("LOAN-SERVICE")
                    .eventType(eventType)
                    .userId(userId)
                    .affectedEntityType(entityType)
                    .affectedEntityId(entityId)
                    .description(description)
                    .statusCode(statusCode)
                    .errorMessage(errorMessage)
                    .build());
            log.debug("Audit log saved: {} for entity {}", eventType, entityId);
        } catch (Exception e) {
            log.error("Failed to save audit log for event {}: {}", eventType, e.getMessage());
        }
    }
}
