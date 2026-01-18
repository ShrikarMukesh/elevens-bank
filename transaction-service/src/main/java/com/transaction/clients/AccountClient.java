package com.transaction.clients;

import com.transaction.dto.AccountTransactionRequest;
import com.transaction.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(
        name = "account-service",
        url = "${account.service.url}" // e.g., http://localhost:3001
)
public interface AccountClient {

    @PostMapping("/api/accounts/{id}/deposit")
    void deposit(@PathVariable("id") Long accountId,
                 @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/accounts/{id}/withdraw")
    void withdraw(@PathVariable("id") Long accountId,
                  @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/accounts/transfer")
    void transfer(@RequestBody AccountTransactionRequest request);

    @GetMapping("/api/accounts/{id}")
    AccountResponse getAccount(@PathVariable("id") Long accountId);

}
