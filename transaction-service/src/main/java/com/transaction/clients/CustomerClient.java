package com.transaction.clients;

import com.transaction.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customer-service",
        url = "${customer.service.url}"
)
public interface CustomerClient {

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable("customerId") String customerId);
}
