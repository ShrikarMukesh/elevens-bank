package com.accounts.repository;

import com.accounts.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // ISP (Interface Segregation Principle):
    // This repository interface exposes only account-related DB operations.
    // It is small, focused, and not a "God repository" with unrelated methods.

    Optional<Account> findByAccountNumber(String accountNumber);
    // SRP: Only fetches accounts by account number.

    Optional<Account> findById(Long id);
    // SRP: Single responsibility – fetch account by ID.

    List<Account> findByCustomerId(String customerId);
    // SRP: Method exists only for fetching accounts by customerId.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
    // SRP: Responsible only for row-level locking when needed.
    // DIP: High-level service doesn't deal with SQL locking details; it depends on this abstraction.

}

/*

✔ ISP — Interface Segregation Principle

This repository is small, focused, and contains only methods related to Account persistence.

It does NOT force unrelated methods on consumers.

Very strong use of ISP.

✔ SRP — Single Responsibility Principle

Each method does exactly one DB operation.

The interface itself has one responsibility: database access for Account entity.

✔ DIP — Dependency Inversion Principle

Controllers/Services depend on the AccountRepository abstraction, not database implementation.

Spring injects the proxy at runtime.

❌ OCP — Not applicable

Repositories rarely show OCP unless using custom compositions.

❌ LSP — Not applicable

No inheritance inside this repository.
 */