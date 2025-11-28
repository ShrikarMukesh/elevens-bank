package com.elevens.loanservice.controller;

import com.elevens.loanservice.model.Loan;
import com.elevens.loanservice.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<Loan> applyLoan(@RequestBody Map<String, Object> request) {
        String customerId = (String) request.get("customerId");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        BigDecimal interestRate = new BigDecimal(request.get("interestRate").toString());
        Integer tenureMonths = (Integer) request.get("tenureMonths");

        return ResponseEntity.ok(loanService.applyLoan(customerId, amount, interestRate, tenureMonths));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<Loan>> getMyLoans(@RequestParam String customerId) {
        return ResponseEntity.ok(loanService.getLoansByCustomer(customerId));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Loan> approveLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.approveLoan(id));
    }
}
