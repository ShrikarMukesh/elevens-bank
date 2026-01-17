package com.customers.kafka;

import com.customers.entity.Customer;
import com.customers.event.CustomerEvent;
import com.customers.event.UserCreatedEvent;
import com.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final CustomerRepository customerRepository;
    private final CustomerEventProducer customerEventProducer;

    @KafkaListener(topics = "${spring.kafka.topic.user-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeUserCreated(UserCreatedEvent event, Acknowledgment ack) {
        try {
            log.info("📩 Received UserCreatedEvent: {}", event);

            if (customerRepository.existsByUserId(event.userId())) {
                log.warn("⚠️ Customer already exists for userId={} — ignoring duplicate event", event.userId());
                ack.acknowledge();
                return;
            }

            // Generate a unique customerId
            String generatedCustomerId = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Split full name into first and last name
            String firstName = event.fullName();
            String lastName = "";
            if (event.fullName() != null && event.fullName().contains(" ")) {
                String[] parts = event.fullName().split(" ", 2);
                firstName = parts[0];
                lastName = parts[1];
            }

            Customer customer = Customer.builder()
                    .customerId(generatedCustomerId)
                    .userId(event.userId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(event.email())
                    .status("ACTIVE")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            customerRepository.save(customer);
            log.info("✅ Customer profile created for userId={} with customerId={}", event.userId(), generatedCustomerId);

            // Publish CustomerCreated event
            CustomerEvent customerEvent = CustomerEvent.builder()
                    .eventType("CUSTOMER_CREATED")
                    .customerId(generatedCustomerId)
                    .userId(event.userId())
                    .verified(false)
                    .verifiedAt(null)
                    .build();
            
            customerEventProducer.sendCustomerEvent(customerEvent);

            ack.acknowledge(); // ✅ safely commit offset
        } catch (Exception e) {
            log.error("❌ Failed to process UserCreatedEvent for userId={} | Error: {}", event.userId(), e.getMessage());
            // Kafka will retry automatically because we didn't ack
        }
    }

}
