package com.elevens.loanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public record LoanRequest(
        String customerId,
        BigDecimal amount,
        BigDecimal interestRate,
        Integer tenureMonths) {
}
