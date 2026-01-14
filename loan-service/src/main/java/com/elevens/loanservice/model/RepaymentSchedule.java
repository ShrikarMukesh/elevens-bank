package com.elevens.loanservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repayment_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    @JsonBackReference // Breaks circular reference during serialization (Child side)
    @ToString.Exclude  // Breaks circular reference during toString()
    private Loan loan;

    private LocalDate dueDate;
    private BigDecimal amount;
    private String status; // PENDING, PAID, OVERDUE
}
