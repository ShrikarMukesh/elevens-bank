# 📚 Database Normalization & The N+1 Problem (Interview Guide)

## 1. What is Normalization?
Normalization is the process of organizing data in a database to **reduce redundancy** and **improve data integrity**. It involves dividing large tables into smaller, related tables and defining relationships between them.

---

## 2. Normal Forms (The "Rules")

### 🟢 First Normal Form (1NF) - "Atomic Values"
*   **Rule:** Each column must contain only atomic (indivisible) values. No repeating groups or arrays.
*   **Violation:** A `Student` table has a column `Subjects` with value `"Math, Science, History"`.
*   **Fix:** Create a separate row for each subject or a separate table.

### 🟡 Second Normal Form (2NF) - "No Partial Dependency"
*   **Prerequisite:** Must be in 1NF.
*   **Rule:** All non-key attributes must depend on the **entire** primary key.
*   **Violation:** In a table with composite key `(StudentID, CourseID)`, a column `StudentName` depends only on `StudentID`, not `CourseID`.
*   **Fix:** Move `StudentName` to a separate `Students` table.

### 🔴 Third Normal Form (3NF) - "No Transitive Dependency"
*   **Prerequisite:** Must be in 2NF.
*   **Rule:** Non-key attributes should not depend on other non-key attributes.
*   **Violation:** A `Loan` table has `ZipCode` and `City`. `City` depends on `ZipCode`, not directly on the `LoanID`.
*   **Fix:** Move `ZipCode` and `City` to a separate `Locations` table.

> **Mnemonic:** "The key, the whole key, and nothing but the key (so help me Codd)."

---

## 3. The N+1 Problem

### ❓ What is it?
The N+1 problem is a performance issue that happens when an application makes **1 database query** to fetch a list of parent entities (e.g., `Loans`) and then **N additional queries** to fetch related child entities (e.g., `RepaymentSchedules`) for *each* parent.

### 📝 Example Scenario
You want to display a list of 10 loans and their repayment schedules.

1.  **Query 1:** `SELECT * FROM loans` (Returns 10 loans).
2.  **Query 2:** `SELECT * FROM repayment_schedules WHERE loan_id = 1`
3.  **Query 3:** `SELECT * FROM repayment_schedules WHERE loan_id = 2`
    ...
11. **Query 11:** `SELECT * FROM repayment_schedules WHERE loan_id = 10`

**Total Queries:** 1 (for list) + 10 (for details) = **11 queries**.
If you have 1,000 loans, you execute **1,001 queries**. This kills database performance.

### 🛠️ How to Fix It?
The goal is to fetch everything in **one single query**.

#### Solution 1: JOIN FETCH (JPQL)
```java
@Query("SELECT l FROM Loan l JOIN FETCH l.repaymentSchedule")
List<Loan> findAllWithSchedules();
```

#### Solution 2: @EntityGraph (Spring Data JPA) - *Used in this project*
```java
@EntityGraph(attributePaths = "repaymentSchedule")
List<Loan> findByCustomerId(String customerId);
```
This tells JPA: "When you run this query, also load the `repaymentSchedule` relationship immediately using a SQL JOIN."

---

## 4. Infinite Recursion (StackOverflowError)
When you have bidirectional relationships (Loan <-> RepaymentSchedule), JSON serializers (like Jackson) get confused:
1.  Serialize Loan.
2.  Loan has Schedules -> Serialize Schedules.
3.  Schedule has Loan -> Serialize Loan.
4.  Loan has Schedules -> Serialize Schedules... **BOOM 💥**

### ✅ The Fix
*   **@JsonManagedReference:** Put this on the **Parent** side (`Loan`). It says "Serialize me normally."
*   **@JsonBackReference:** Put this on the **Child** side (`RepaymentSchedule`). It says "Don't serialize this field, just ignore it."
