package com.elevens.loanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanRequest {
    private String customerId;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
}
