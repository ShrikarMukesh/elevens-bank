package com.customers.kafka;


import com.customers.entity.Customer;
import com.customers.event.UserCreatedEvent;
import com.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final CustomerRepository customerRepository;

    @KafkaListener(
            topics = "bank.user.event.v1",
            groupId = "customer-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserCreated(UserCreatedEvent event, Acknowledgment ack) {
        try {
            log.info("📩 Received UserCreatedEvent: {}", event);

            if (customerRepository.existsByUserId(event.getUserId())) {
                log.warn("⚠️ Customer already exists for userId={} — ignoring duplicate event", event.getUserId());
                ack.acknowledge();
                return;
            }

            Customer customer = Customer.builder()
                    .userId(event.getUserId())
                    .firstName(event.getFullName())
                    .email(event.getEmail())
                    .status("ACTIVE")
                    .createdAt(Instant.now())
                    .build();

            customerRepository.save(customer);
            log.info("✅ Customer profile created for userId={}", event.getUserId());

            ack.acknowledge(); // ✅ safely commit offset
        } catch (Exception e) {
            log.error("❌ Failed to process UserCreatedEvent for userId={} | Error: {}", event.getUserId(), e.getMessage());
            // Kafka will retry automatically because we didn't ack
        }
    }

}
