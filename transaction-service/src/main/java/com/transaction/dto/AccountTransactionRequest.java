package com.transaction.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransactionRequest {
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
}

