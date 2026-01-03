package com.accounts.dto;

import com.accounts.entity.AccountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

public record AccountRequest(
        @NotNull(message = "Customer ID is required") String customerId,

        @NotBlank(message = "Account number is required") @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 characters") String accountNumber,

        @NotNull(message = "Account type is required") AccountType accountType,

        @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative") BigDecimal balance,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase code") String currency) {
    public AccountRequest {
        if (balance == null)
            balance = BigDecimal.ZERO;
        if (currency == null)
            currency = "INR";
    }
}
