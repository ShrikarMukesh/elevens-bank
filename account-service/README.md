# Account Service

## Overview
The `account-service` is the core banking ledger. It manages user accounts, balances, and executes atomic financial operations. It is designed for high consistency (ACID) and high read throughput.

## Key Features
- **Account Management**: Create, Read, Update, Delete (CRUD) for accounts.
- **Caching**: Uses **Redis** (Look-aside pattern) to cache account details (`@Cacheable`).
- **Concurrency Control**: Implements **Pessimistic Locking** (`select for update`) for safe money transfers.
- **Optimistic Locking**: Uses `@Version` to prevent lost updates on entity modifications.

## Tech Stack
- **Database**: MySQL (Relational, ACID compliant)
- **Caching**: Redis
- **Framework**: Spring Data JPA

## Flow Diagrams

### 1. Money Transfer Flow (Internal)
This flow illustrates how the service handles concurrency during a transfer.

```mermaid
sequenceDiagram
    participant TransactionService
    participant AccountService
    participant DB as MySQL Database

    TransactionService->>AccountService: transfer(fromId, toId, amount)
    
    note right of AccountService: 1. Order Locks (Deadlock Prevention)
    AccountService->>DB: findByIdForUpdate(firstId)
    DB-->>AccountService: Locked Account A
    AccountService->>DB: findByIdForUpdate(secondId)
    DB-->>AccountService: Locked Account B

    note right of AccountService: 2. Core Business Logic
    AccountService->>AccountService: checkBalance(fromAccount)
    AccountService->>AccountService: debit(fromAccount)
    AccountService->>AccountService: credit(toAccount)
    
    AccountService->>DB: save(fromAccount)
    AccountService->>DB: save(toAccount)
    
    AccountService-->>TransactionService: Success
```

### 2. Get Account (Caching Strategy)
```mermaid
graph TD
    A[Client Request] --> B{Check Redis Cache?}
    B -- Hit --> C[Return Cached Data]
    B -- Miss --> D[Query MySQL DB]
    D --> E[Store in Redis]
    E --> F[Return Data]
```
