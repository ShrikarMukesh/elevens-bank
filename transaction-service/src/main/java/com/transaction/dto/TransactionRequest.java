package com.transaction.dto;

import com.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    @NotNull
    private Long accountId;

    private Long targetAccountId; // For transfers

    @NotNull
    private BigDecimal amount;

    @NotNull
    private TransactionType transactionType;

    @NotNull
    private Integer modeId;

    private String description;
}
