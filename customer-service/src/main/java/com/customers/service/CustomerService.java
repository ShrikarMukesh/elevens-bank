package com.customers.service;

import com.customers.model.CustomerDto;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
    CustomerDto createCustomer(CustomerDto customer);

    Optional<CustomerDto> getCustomerById(String customerId);

    Optional<CustomerDto> getCustomerByEmail(String email);

    List<CustomerDto> getCustomersByStatus(String status);

    CustomerDto updateCustomer(String customerId, CustomerDto updatedData);

    void deleteCustomer(String customerId);

    void updateKycStatus(String customerId, boolean verified);
}
