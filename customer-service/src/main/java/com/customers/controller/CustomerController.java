package com.customers.controller;

import com.customers.model.CustomerDto;
import com.customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CustomerController implements com.customers.api.CustomersApi {

    private final CustomerService customerService;

    @Override
    public ResponseEntity<CustomerDto> createCustomer(CustomerDto customerDto) {
        log.info("Received request to create customer: {}", customerDto.getEmail());
        CustomerDto createdCustomer = customerService.createCustomer(customerDto);
        return ResponseEntity.ok(createdCustomer);
    }

    @Override
    public ResponseEntity<CustomerDto> getCustomerById(String customerId) {
        return customerService.getCustomerById(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<CustomerDto> getCustomerByEmail(String email) {
        return customerService.getCustomerByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<CustomerDto>> getCustomersByStatus(String status) {
        return ResponseEntity.ok(customerService.getCustomersByStatus(status));
    }

    @Override
    public ResponseEntity<CustomerDto> updateCustomer(String customerId, CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, customerDto));
    }

    @Override
    public ResponseEntity<String> updateKycStatus(String customerId, Boolean verified) {
        customerService.updateKycStatus(customerId, verified);
        return ResponseEntity.ok("KYC updated successfully for " + customerId);
    }

    @Override
    public ResponseEntity<String> deleteCustomer(String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok("Customer deleted successfully");
    }
}
