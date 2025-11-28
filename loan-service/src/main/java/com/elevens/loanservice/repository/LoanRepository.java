package com.elevens.loanservice.repository;

import com.elevens.loanservice.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByCustomerId(String customerId);
}
