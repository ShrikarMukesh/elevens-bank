package com.elevens.loanservice.service;

import com.elevens.loanservice.model.Loan;
import com.elevens.loanservice.model.LoanStatus;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Loan applyLoan(String customerId, BigDecimal amount, BigDecimal interestRate, Integer tenureMonths) {
        Loan loan = Loan.builder()
                .customerId(customerId)
                .amount(amount)
                .interestRate(interestRate)
                .tenureMonths(tenureMonths)
                .status(LoanStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();
        return loanRepository.save(loan);
    }

    public List<Loan> getLoansByCustomer(String customerId) {
        return loanRepository.findByCustomerId(customerId);
    }

    public Loan approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());
        
        Loan savedLoan = loanRepository.save(loan);
        
        // Publish event to Kafka
        publishLoanEvent(savedLoan, "LOAN_DISBURSED");
        
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
}
