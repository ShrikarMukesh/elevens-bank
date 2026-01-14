package com.elevens.loanservice.repository;

import com.elevens.loanservice.model.Loan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    /**
     * <h3>Normalization & N+1 Solution</h3>
     * <p>
     * <b>Normalization Context:</b> The database schema is normalized to <b>3NF</b>.
     * <ul>
     *   <li><b>1NF:</b> All columns are atomic (no comma-separated lists).</li>
     *   <li><b>2NF:</b> All non-key attributes depend on the whole primary key.</li>
     *   <li><b>3NF:</b> No transitive dependencies (e.g., customer details are in Customer Service, not duplicated here).</li>
     * </ul>
     * Because 'RepaymentSchedule' is a separate entity (1:N relationship), fetching a list of Loans
     * normally triggers lazy loading for schedules, causing the <b>N+1 Problem</b>.
     * </p>
     * <p>
     * <b>The Fix:</b> We use <code>@EntityGraph</code> to perform a <b>JOIN FETCH</b>.
     * This retrieves the Loan AND its RepaymentSchedules in a <b>single SQL query</b>,
     * eliminating the N+1 performance issue.
     * </p>
     */
    @EntityGraph(attributePaths = "repaymentSchedule")
    List<Loan> findByCustomerId(String customerId);
}
