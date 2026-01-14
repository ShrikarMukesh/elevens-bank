# System Design Interview: Database & Schema Cheatsheet

This guide covers the key database concepts you'll be expected to know in a system design interview, from high-level strategy to practical SQL knowledge.

---

## 1. Choosing the Right Database: SQL vs. NoSQL

This is the foundational decision. The key is to justify your choice based on the system's requirements. Don't forget to mention **Polyglot Persistence**—using multiple databases for different services.

| Dimension | SQL (e.g., PostgreSQL, MySQL) | NoSQL (e.g., MongoDB, Cassandra) |
| :--- | :--- | :--- |
| **Data Model** | Structured, relational data with a predefined schema. | Semi-structured or unstructured data with a flexible, dynamic schema. |
| **Consistency** | **ACID** (Atomicity, Consistency, Isolation, Durability). Guarantees reliability for transactions. | **BASE** (Basically Available, Soft state, Eventual consistency). Prioritizes availability over immediate consistency. |
| **Scalability** | **Vertical Scaling** (buy a bigger server). Read-heavy loads can be scaled with **Read Replicas**. | **Horizontal Scaling** (add more servers). Built for massive scale from the ground up. |
| **Queries** | Handles complex queries with `JOIN`s and aggregations efficiently. | Best for simple queries by key or index. `JOIN`s are often not supported or are very slow. |
| **Use Case** | Core transactional systems: financial ledgers, e-commerce orders, booking systems. | Big Data applications: user profiles, IoT data, real-time analytics, content management. |

**Example (`elevens-bank`):**
- **SQL** for `accounts`, `transactions`, `loans` (requires strong consistency).
- **NoSQL (MongoDB)** for `customers` (flexible profile data).
- **In-Memory (Redis)** for `sessions` (fast access, automatic expiry).

---

## 2. Schema Design & Data Modeling

This is where you translate requirements into tables or documents.

### Key Trade-off: Normalization vs. Denormalization

- **Normalization (SQL Default):**
  - **Goal:** Reduce data redundancy by splitting data into multiple tables (e.g., a `products` table has a `category_id` that links to a `categories` table).
  - **Pros:** High data integrity, less storage, simpler writes.
  - **Cons:** Reads require expensive `JOIN`s.

- **Denormalization (NoSQL Default):**
  - **Goal:** Speed up reads by duplicating data (e.g., a `product` document includes the category name directly).
  - **Pros:** Extremely fast reads (no `JOIN`s needed).
  - **Cons:** Can lead to inconsistent data if updates aren't handled carefully, uses more storage.

### Modeling Relationships

- **One-to-Many (1:M):** A `Loan` has many `RepaymentSchedules`.
  - **Implementation:** Use a foreign key (`loan_id` in the `repayment_schedules` table).
- **Many-to-Many (M:M):** A `User` can have many `Roles`, and a `Role` can be assigned to many `Users`.
  - **Implementation:** Requires a **join table** (e.g., `user_roles` with `user_id` and `role_id`).

### Schema Evolution

- **Problem:** How do you add a column to a production database with zero downtime?
- **Solution:**
  1. Use a migration tool (e.g., Flyway, Liquibase).
  2. Make the change backward-compatible (e.g., add the new column as `NULL`-able or with a `DEFAULT` value).
  3. Deploy the application code that can handle the new column.
  4. Run the migration to alter the table.

---

## 3. Scalability & Performance

How do you handle millions of users?

### 1. Indexing (The First Answer)

- **What it is:** A data structure (usually a B-Tree) that allows the database to find rows without scanning the entire table.
- **What to Index:**
  - Foreign Keys (`customer_id`, `account_id`).
  - Columns used frequently in `WHERE` clauses.
  - Columns used in `ORDER BY` clauses.
- **Trade-off:** Indexes dramatically speed up reads (`SELECT`) but slow down writes (`INSERT`, `UPDATE`, `DELETE`) because the index must also be updated.

### 2. Read Replicas (Replication)

- **What it is:** Creating read-only copies of the primary database.
- **How it works:** The application writes to the primary and directs read queries to the replicas.
- **Use Case:** Scaling read-heavy workloads.
- **Trade-off:** **Replication Lag**. Data on the replicas might be slightly stale. This is a form of eventual consistency.

### 3. Sharding (Horizontal Partitioning)

- **What it is:** Splitting a large table into smaller pieces (shards) and distributing them across multiple database servers.
- **Use Case:** When data is too large for one server or write throughput is a bottleneck.
- **Key Challenge:** Choosing a good **Shard Key** (e.g., `customer_id`). A bad key can create "hotspots" (one server gets all the traffic). Cross-shard `JOIN`s are extremely difficult and should be avoided.

---

## 4. Practical SQL Knowledge

These topics demonstrate real-world experience.

### The N+1 Query Problem

- **Problem:** Fetching a list of items (1 query) and then looping through them to fetch a related item for each one (N queries).
  ```java
  // 1 query to get all loans
  List<Loan> loans = loanRepository.findAll(); 
  // N queries to get the repayment schedule for each loan
  for (Loan loan : loans) {
      List<RepaymentSchedule> schedule = loan.getRepaymentSchedule(); // Triggers a query
  }
  ```
- **Solution:** Use a `JOIN` in a single query to fetch all the required data at once. In JPA, this can be done with `@EntityGraph` or a `JOIN FETCH` query.

### Query Analysis

- **Question:** "A query is slow. How do you investigate?"
- **Answer:**
  1. Use the `EXPLAIN` command (e.g., `EXPLAIN SELECT * FROM loans WHERE customer_id = 'CUST123';`).
  2. Look at the output for a **Full Table Scan**, which indicates a missing index.

### Concurrency Control

- **Pessimistic Locking:**
  - **How:** Lock the row when you read it (`SELECT ... FOR UPDATE`). No other transaction can touch it until you're done.
  - **When:** Use when conflicts are frequent and data integrity is critical (e.g., updating an account balance).

- **Optimistic Locking:**
  - **How:** Add a `version` column to the table. When updating, check if the `version` has changed since you read it. If it has, the transaction fails and must be retried.
  - **When:** Use when conflicts are rare and you want to avoid the overhead of database locks (e.g., updating a customer's profile information).
```