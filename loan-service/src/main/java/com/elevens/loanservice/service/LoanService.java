package com.elevens.loanservice.service;

import com.elevens.loanservice.model.Loan;
import com.elevens.loanservice.model.LoanStatus;
import com.elevens.loanservice.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;

    public Loan applyLoan(String customerId, BigDecimal amount, BigDecimal interestRate, Integer tenureMonths) {
        Loan loan = Loan.builder()
                .customerId(customerId)
                .amount(amount)
                .interestRate(interestRate)
                .tenureMonths(tenureMonths)
                .status(LoanStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();
        return loanRepository.save(loan);
    }

    public List<Loan> getLoansByCustomer(String customerId) {
        return loanRepository.findByCustomerId(customerId);
    }

    public Loan approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }
}
