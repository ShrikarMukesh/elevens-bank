package com.accounts.dto;

import com.accounts.entity.AccountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Account number is required")
    @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 characters")
    private String accountNumber;

    @NotNull(message = "Account type is required")
    private AccountType accountType;  // Use Enum directly

    @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
    private BigDecimal balance = BigDecimal.ZERO; // default to 0

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase code")
    private String currency = "INR"; // default to INR
}
