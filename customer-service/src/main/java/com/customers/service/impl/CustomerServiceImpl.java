package com.customers.service.impl;

import com.customers.entity.AuditLog;
import com.customers.entity.Customer;
import com.customers.event.CustomerEvent;
import com.customers.exception.BadRequestException;
import com.customers.exception.ResourceNotFoundException;
import com.customers.kafka.CustomerEventProducer;
import com.customers.mapper.CustomerMapper;
import com.customers.model.CustomerDto;
import com.customers.repository.AuditLogRepository;
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
    private final AuditLogRepository auditLogRepository;
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

        // Audit Log: CUSTOMER_CREATED
        saveAuditLog("CUSTOMER_CREATED", saved.getUserId(), "CUSTOMER", saved.getCustomerId(),
                "Customer created successfully", 201, null);

        // Publish Kafka Event after creation
        CustomerEvent event = CustomerEvent.builder()
                .eventType("CUSTOMER_CREATED")
                .customerId(saved.getCustomerId())
                .userId(saved.getUserId())
                .verified("VERIFIED".equals(saved.getKyc() != null ? saved.getKyc().getStatus() : null))
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
    public Optional<CustomerDto> getCustomerByUserId(String userId) {
        return customerRepository.findByUserId(userId)
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
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

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

        // Audit Log: CUSTOMER_UPDATED
        saveAuditLog("CUSTOMER_UPDATED", saved.getUserId(), "CUSTOMER", saved.getCustomerId(),
                "Customer profile updated", 200, null);

        return customerMapper.toDto(saved);
    }

    @Override
    public void deleteCustomer(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElse(null);

        if (customer != null) {
            customerRepository.delete(customer);
            // Audit Log: CUSTOMER_DELETED
            saveAuditLog("CUSTOMER_DELETED", customer.getUserId(), "CUSTOMER", customerId,
                    "Customer deleted", 200, null);
        }
    }

    @Override
    public void updateKycStatus(String customerId, boolean verified) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        if (customer.getKyc() == null) {
            throw new BadRequestException("KYC details not found for customer: " + customerId);
        }

        customer.getKyc().setStatus(verified ? "VERIFIED" : "PENDING");
        if (verified) {
            customer.getKyc().setVerifiedAt(Instant.now());
        }
        customerRepository.save(customer);

        // Audit Log: KYC_STATUS_UPDATED
        saveAuditLog("KYC_STATUS_UPDATED", customer.getUserId(), "CUSTOMER", customerId,
                "KYC status updated to: " + (verified ? "VERIFIED" : "PENDING"), 200, null);

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

    /**
     * Saves an audit log entry for customer events.
     * Wrapped in try-catch to prevent audit failures from affecting business logic.
     */
    private void saveAuditLog(String eventType, String userId, String entityType, String entityId,
            String description, Integer statusCode, String errorMessage) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .serviceName("CUSTOMER-SERVICE")
                    .eventType(eventType)
                    .userId(userId)
                    .affectedEntityType(entityType)
                    .affectedEntityId(entityId)
                    .description(description)
                    .statusCode(statusCode)
                    .errorMessage(errorMessage)
                    .createdAt(Instant.now())
                    .build());
            log.debug("Audit log saved: {} for entity {}", eventType, entityId);
        } catch (Exception e) {
            log.error("Failed to save audit log for event {}: {}", eventType, e.getMessage());
        }
    }
}
