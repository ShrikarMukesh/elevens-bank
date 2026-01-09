package com.customers.service.impl;

import com.customers.entity.Customer;
import com.customers.event.CustomerEvent;
import com.customers.kafka.CustomerEventProducer;
import com.customers.mapper.CustomerMapper;
import com.customers.model.CustomerDto;
import com.customers.repository.CustomerRepository;
import com.customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerEventProducer eventProducer;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerDto createCustomer(CustomerDto customerDto) {
        log.info("Creating customer: {}", customerDto.getEmail());

        if (customerRepository.existsByEmail(customerDto.getEmail())) {
            throw new DuplicateKeyException("Customer with email already exists: " + customerDto.getEmail());
        }

        Customer entity = customerMapper.toEntity(customerDto);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setStatus("ACTIVE");

        Customer saved = customerRepository.save(entity);

        // Publish Kafka Event after creation
        CustomerEvent event = CustomerEvent.builder()
                .eventType("CUSTOMER_CREATED")
                .customerId(saved.getCustomerId())
                .userId(saved.getUserId())
                .verified(saved.getKyc() != null && saved.getKyc().isVerified())
                .verifiedAt(saved.getKyc() != null ? saved.getKyc().getVerifiedAt() : null)
                .build();

        eventProducer.sendCustomerEvent(event);
        log.info("Kafka Event Published: {}", event);

        return customerMapper.toDto(saved);
    }

    @Override
    public Optional<CustomerDto> getCustomerById(String customerId) {
        return customerRepository.findByCustomerId(customerId)
                .map(customerMapper::toDto);
    }

    @Override
    public Optional<CustomerDto> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .map(customerMapper::toDto);
    }

    @Override
    public List<CustomerDto> getCustomersByStatus(String status) {
        return customerRepository.findByStatus(status).stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDto updateCustomer(String customerId, CustomerDto updatedDto) {
        Customer existing = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        Customer updatedEntity = customerMapper.toEntity(updatedDto);

        if (updatedEntity.getFirstName() != null)
            existing.setFirstName(updatedEntity.getFirstName());
        if (updatedEntity.getLastName() != null)
            existing.setLastName(updatedEntity.getLastName());
        if (updatedEntity.getEmail() != null)
            existing.setEmail(updatedEntity.getEmail());
        if (updatedEntity.getAddresses() != null)
            existing.setAddresses(updatedEntity.getAddresses());
        if (updatedEntity.getPhoneNumbers() != null)
            existing.setPhoneNumbers(updatedEntity.getPhoneNumbers());
        if (updatedEntity.getKyc() != null)
            existing.setKyc(updatedEntity.getKyc());
        if (updatedEntity.getPreferences() != null)
            existing.setPreferences(updatedEntity.getPreferences());

        existing.setUpdatedAt(Instant.now());
        Customer saved = customerRepository.save(existing);
        return customerMapper.toDto(saved);
    }

    @Override
    public void deleteCustomer(String customerId) {
        customerRepository.findByCustomerId(customerId)
                .ifPresent(customerRepository::delete);
    }

    @Override
    public void updateKycStatus(String customerId, boolean verified) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        if (customer.getKyc() == null) {
            throw new RuntimeException("KYC details not found for customer: " + customerId);
        }

        customer.getKyc().setVerified(verified);
        customer.getKyc().setVerifiedAt(Instant.now());
        customerRepository.save(customer);

        log.info("Updated KYC for customer {} to {}", customerId, verified);

        // Publish Kafka Event after KYC verification
        CustomerEvent event = CustomerEvent.builder()
                .eventType("CUSTOMER_VERIFIED")
                .customerId(customerId)
                .userId(customer.getUserId())
                .verified(verified)
                .verifiedAt(customer.getKyc().getVerifiedAt())
                .build();

        eventProducer.sendCustomerEvent(event);
        log.info("Kafka Event Published: {}", event);
    }
}
