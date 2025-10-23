package com.accounts.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountTransactionRequest {
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
}
