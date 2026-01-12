# Transaction Management in Spring Boot

This guide explains Transaction Propagation and Isolation levels with real-world scenarios relevant to the **Elevens Bank** project.

---

## 1. Propagation (`Propagation.*`)
Propagation defines what happens when one transactional method calls another.

| Type | Behavior | Real-World Use Case |
| :--- | :--- | :--- |
| **`REQUIRED`** (Default) | **"Join or Create"**<br>If a transaction exists, join it. If not, create a new one. | **General Business Logic.** Most service methods use this so they all succeed or fail together. |
| **`REQUIRES_NEW`** | **"Always New"**<br>Always creates a new transaction. If one exists, it is **suspended** until the new one finishes. | **Audit Logs / Attempts.** Even if the main payment fails (rollback), you still want to save the "Attempted Payment" log to the DB. |
| **`SUPPORTS`** | **"Go with the flow"**<br>If a transaction exists, use it. If not, run non-transactionally. | **Read-only operations.** Generating a report that doesn't strictly need a transaction but can use one if available. |
| **`NOT_SUPPORTED`** | **"Pause and Run"**<br>Suspends the current transaction (if any) and runs non-transactionally. | **Heavy processing.** Sending an email or processing a large file where you don't want to hold a database lock open. |
| **`MANDATORY`** | **"Must have one"**<br>Throw an exception if no active transaction exists. | **Internal helper methods.** A method that updates a balance and *must* be part of a larger transfer flow. |
| **`NEVER`** | **"Must NOT have one"**<br>Throw an exception if an active transaction exists. | **Non-transactional tasks.** Operations that are explicitly not thread-safe or compatible with DB transactions. |
| **`NESTED`** | **"Sub-transaction"**<br>Runs inside the existing transaction. If the inner one fails, it rolls back to a "savepoint," but the outer one can continue. | **Complex batch jobs.** If one record fails, rollback just that record, but keep processing the rest of the batch. |

---

## 2. Isolation (`Isolation.*`)
Isolation defines how data is locked and visible between concurrent transactions.

**Key Side Effects:**
*   **Dirty Read:** Reading data that hasn't been committed yet (and might be rolled back).
*   **Non-Repeatable Read:** Reading the same row twice gets different results (someone updated it in between).
*   **Phantom Read:** Running a query twice gets a different *number* of rows (someone inserted/deleted a row in between).

| Level | Strictness | Behavior | Performance |
| :--- | :--- | :--- | :--- |
| **`DEFAULT`** | N/A | Uses the database's default setting. (Read Committed for Postgres/Oracle/SQL Server; Repeatable Read for MySQL). | N/A |
| **`READ_UNCOMMITTED`** | Lowest | **"The Wild West"**<br>Allows Dirty Reads. You can see data that isn't committed yet. | Fastest / Unsafe |
| **`READ_COMMITTED`** | Medium | **"The Standard"**<br>Prevents Dirty Reads. You only see data after it is committed. However, data can change between two reads in the same transaction. | Good / Standard |
| **`REPEATABLE_READ`** | High | **"Snapshot"**<br>Prevents Dirty & Non-Repeatable Reads. If you read a row once, it stays the same for the duration of your transaction, even if others update it. | Slower |
| **`SERIALIZABLE`** | Highest | **"Single File Line"**<br>Prevents all side effects (including Phantoms). Transactions are executed sequentially (or appear to be). | Slowest / Safest |

---

## 3. Applied Scenarios in Elevens Bank

### A. Audit Trail / Transaction Attempts (`REQUIRES_NEW`)
**File:** `TransactionService.java`
**Method:** `createTransactionRecord`

We use `REQUIRES_NEW` here to ensure the "PENDING" transaction record is committed immediately.
*   **Why?** If the subsequent call to the Account Service fails (e.g., network error), the main transaction might roll back.
*   **Benefit:** By committing the "PENDING" record in its own transaction, we preserve the audit trail. We can then catch the exception and update the status to "FAILED" instead of losing the record entirely.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Transaction createTransactionRecord(TransactionRequest request) {
    // ... saves PENDING transaction ...
}
```

### B. Consistent Reads (`READ_COMMITTED`)
**File:** `TransactionService.java`
**Method:** `getTransactionsByAccountId`

We use `READ_COMMITTED` (often the default) combined with `readOnly = true`.
*   **Why?** To ensure users only see finalized transactions. We don't want them to see a "Deposit" that is currently processing and might fail.
*   **Benefit:** Prevents "Dirty Reads" while maintaining good performance.

```java
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public List<Transaction> getTransactionsByAccountId(Long accountId) {
    // ... fetches history ...
}
```

### C. Helper Methods (`MANDATORY`)
*Note: Not currently implemented but a good candidate.*
**Method:** `callAccountServiceWithRetry`

If we wanted to strictly enforce that this method is only called within an existing transaction context (e.g., inside `performTransaction`), we could use `MANDATORY`.

```java
@Transactional(propagation = Propagation.MANDATORY)
public void callAccountServiceWithRetry(TransactionRequest request) {
    // ... business logic ...
}
```
