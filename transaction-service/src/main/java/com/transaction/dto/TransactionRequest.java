package com.transaction.dto;

import com.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull Long accountId,
        Long targetAccountId,
        @NotNull BigDecimal amount,
        @NotNull TransactionType transactionType,
        @NotNull Integer modeId,
        String description) {
}
