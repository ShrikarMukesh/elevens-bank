package com.transaction.dto;

import com.transaction.entity.TransactionMode;
import com.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull Long accountId,
        Long targetAccountId,
        @NotNull BigDecimal amount,
        @NotNull TransactionType transactionType,
        @NotNull TransactionMode transactionMode,
        String description) {
}
