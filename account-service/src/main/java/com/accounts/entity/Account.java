package com.accounts.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_customer_id", columnList = "customerId"),
        @Index(name = "idx_account_status", columnList = "status"),
        @Index(name = "idx_branch_code", columnList = "branchCode")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Column(nullable = false)
    private String customerId;

    private Long userId;

    @Column(unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(length = 3)
    private String currency = "INR";

    @Column(length = 10)
    private String branchCode;

    @Column(precision = 5, scale = 2)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    private String nomineeName;
    private String nomineeRelation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    private LocalDateTime accountOpenedDate;
    private LocalDateTime accountClosedDate;

    private String createdBy;
    private String updatedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Version
    private Long version; // 🔹 Optimistic Locking

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = AccountStatus.ACTIVE;
        if (currency == null) currency = "INR";
        if (balance == null) balance = BigDecimal.ZERO;
        if (availableBalance == null) availableBalance = balance; // ✅ important line
        if (interestRate == null) interestRate = BigDecimal.ZERO;
        if (overdraftLimit == null) overdraftLimit = BigDecimal.ZERO;
        if (accountOpenedDate == null) accountOpenedDate = LocalDateTime.now();
        if (version == null) version = 0L; // ✅ prevent version null
    }

}
