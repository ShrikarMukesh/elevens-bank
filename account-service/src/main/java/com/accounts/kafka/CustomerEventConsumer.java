package com.accounts.kafka;

import com.accounts.dto.CustomerEvent;
import com.accounts.entity.Account;
import com.accounts.entity.AccountStatus;
import com.accounts.entity.AccountType;
import com.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerEventConsumer {

    private final AccountRepository accountRepository;

    @KafkaListener(topics = "${spring.kafka.topic.customer-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(CustomerEvent event) {
        log.info("Received Kafka Event: {}", event);

        if ("CUSTOMER_VERIFIED".equals(event.getEventType())) {
            handleCustomerVerified(event);
        } else if ("CUSTOMER_CREATED".equals(event.getEventType())) {
            log.info("Customer created event received (no account yet): {}", event.getCustomerId());
        } else {
            log.warn("Unhandled event type: {}", event.getEventType());
        }
    }

    private void handleCustomerVerified(CustomerEvent event) {
        // Check if account already exists for this customer
        boolean exists = !accountRepository.findByCustomerId(event.getCustomerId()).isEmpty();
        if (exists) {
            log.info("Account already exists for customer {}", event.getCustomerId());
            return;
        }

        // Create default Savings Account
        Account account = Account.builder()
                .customerId(event.getCustomerId())
                .accountNumber(generateAccountNumber())
                .accountType(AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        accountRepository.save(account);
        log.info("✅ Default savings account created for verified customer: {}", event.getCustomerId());
    }

    private String generateAccountNumber() {
        return "AC" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
