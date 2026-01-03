package com.accounts.dto;

import java.math.BigDecimal;

public record AccountTransactionRequest(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount) {
}
