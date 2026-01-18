package com.transaction.dto;

import java.math.BigDecimal;

public record AccountResponse(
    Long accountId,
    String customerId,
    String accountNumber,
    String accountType,
    BigDecimal balance,
    String currency,
    String status
) {}
