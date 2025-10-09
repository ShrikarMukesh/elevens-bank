package com.customers.controller;

import com.customers.entity.Customer;
import com.customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        log.info("Received request to create customer: {}", customer.getEmail());
        return ResponseEntity.ok(customerService.createCustomer(customer));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String customerId) {
        return customerService.getCustomerById(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Customer> getCustomerByEmail(@PathVariable String email) {
        return customerService.getCustomerByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Customer>> getCustomersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(customerService.getCustomersByStatus(status));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable String customerId, @RequestBody Customer updatedData) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, updatedData));
    }

    @PatchMapping("/{customerId}/kyc")
    public ResponseEntity<String> updateKycStatus(
            @PathVariable String customerId, @RequestParam boolean verified) {
        customerService.updateKycStatus(customerId, verified);
        return ResponseEntity.ok("KYC updated successfully for " + customerId);
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<String> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok("Customer deleted successfully");
    }
}
