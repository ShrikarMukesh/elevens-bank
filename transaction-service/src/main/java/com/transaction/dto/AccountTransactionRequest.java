package com.transaction.dto;

import lombok.*;
import java.math.BigDecimal;

public record AccountTransactionRequest(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount) {
}
