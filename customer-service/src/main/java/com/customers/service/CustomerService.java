package com.customers.service;

import com.customers.entity.Customer;
import com.customers.event.CustomerEvent;
import com.customers.kafka.CustomerEventProducer;
import com.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerEventProducer eventProducer; // 🔥 Inject Kafka producer

    public Customer createCustomer(Customer customer) {
        log.info("Creating customer: {}", customer.getEmail());

        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateKeyException("Customer with email already exists: " + customer.getEmail());
        }

        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        customer.setStatus("ACTIVE");

        Customer saved = customerRepository.save(customer);

        // 🔥 Publish Kafka Event after creation
        CustomerEvent event = CustomerEvent.builder()
                .eventType("CUSTOMER_CREATED")
                .customerId(saved.getCustomerId())
                .userId(String.valueOf(saved.getUserId()))
                .verified(saved.getKyc() != null && saved.getKyc().isVerified())
                .verifiedAt(saved.getKyc() != null ? saved.getKyc().getVerifiedAt() : null)
                .build();

        eventProducer.sendCustomerEvent(event);
        log.info("Kafka Event Published: {}", event);

        return saved;
    }

    public Optional<Customer> getCustomerById(String customerId) {
        return customerRepository.findByCustomerId(customerId);
    }

    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public List<Customer> getCustomersByStatus(String status) {
        return customerRepository.findByStatus(status);
    }

    public Customer updateCustomer(String customerId, Customer updatedData) {
        Customer existing = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        if (updatedData.getFirstName() != null)
            existing.setFirstName(updatedData.getFirstName());
        if (updatedData.getLastName() != null)
            existing.setLastName(updatedData.getLastName());
        if (updatedData.getEmail() != null)
            existing.setEmail(updatedData.getEmail());
        if (updatedData.getAddresses() != null)
            existing.setAddresses(updatedData.getAddresses());
        if (updatedData.getPhoneNumbers() != null)
            existing.setPhoneNumbers(updatedData.getPhoneNumbers());
        if (updatedData.getKyc() != null)
            existing.setKyc(updatedData.getKyc());
        if (updatedData.getPreferences() != null)
            existing.setPreferences(updatedData.getPreferences());

        existing.setUpdatedAt(Instant.now());
        return customerRepository.save(existing);
    }

    public void deleteCustomer(String customerId) {
        customerRepository.findByCustomerId(customerId)
                .ifPresent(customerRepository::delete);
    }

    public void updateKycStatus(String customerId, boolean verified) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        customer.getKyc().setVerified(verified);
        customer.getKyc().setVerifiedAt(Instant.now());
        customerRepository.save(customer);

        log.info("Updated KYC for customer {} to {}", customerId, verified);

        // 🔥 Publish Kafka Event after KYC verification
        CustomerEvent event = CustomerEvent.builder()
                .eventType("CUSTOMER_VERIFIED")
                .customerId(customerId)
                .userId(String.valueOf(customer.getUserId()))
                .verified(verified)
                .verifiedAt(customer.getKyc().getVerifiedAt())
                .build();

        eventProducer.sendCustomerEvent(event);
        log.info("Kafka Event Published: {}", event);
    }
}
