package com.customers.kafka;

import com.customers.entity.Customer;
import com.customers.entity.PhoneNumber;
import com.customers.event.CustomerEvent;
import com.customers.event.UserCreatedEvent;
import com.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final CustomerRepository customerRepository;
    private final CustomerEventProducer customerEventProducer;

    @KafkaListener(topics = "${spring.kafka.topic.user-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeUserCreated(UserCreatedEvent event) {
        log.info("📩 Received UserCreatedEvent: {}", event);

        if (customerRepository.existsByUserId(event.userId())) {
            log.warn("⚠️ Customer already exists for userId={} — ignoring duplicate event", event.userId());
            return;
        }

        // Generate a unique customerId
        String generatedCustomerId = "ELEVEN" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // Split full name into first and last name
        String firstName = event.fullName();
        String lastName = "";
        if (event.fullName() != null && event.fullName().contains(" ")) {
            String[] parts = event.fullName().split(" ", 2);
            firstName = parts[0];
            lastName = parts[1];
        }

        // Handle phone number conversion
        List<PhoneNumber> phoneNumbers = null;
        if (event.phone() != null && !event.phone().isEmpty()) {
            PhoneNumber phoneNumber = new PhoneNumber();
            phoneNumber.setNumber(event.phone());
            phoneNumber.setType("MOBILE"); // Default type
            phoneNumber.setPrimary(true);
            phoneNumbers = Collections.singletonList(phoneNumber);
        }

        Customer customer = Customer.builder()
                .customerId(generatedCustomerId)
                .userId(event.userId())
                .firstName(firstName)
                .lastName(lastName)
                .email(event.email())
                .phoneNumbers(phoneNumbers)
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
    }

}
