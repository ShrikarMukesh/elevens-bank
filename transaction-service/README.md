# Transaction Service

## Overview
The `transaction-service` orchestrates financial transactions across the system. It is responsible for initiating transfers, deposits, and withdrawals. It implements the **Saga Pattern** to ensure distributed data consistency between its own ledger and the `account-service`.

## Key Features
- **Saga Orchestration**: Manages the lifecycle of a distributed transaction (Pending -> Success/Failed).
- **Idempotency**: Prevents duplicate transactions using unique Reference IDs.
- **Resiliency**: Uses **Retry Pattern** with Exponential Backoff for transient failures (e.g., network glitches, database locks).
- **History**: Provides transaction history for accounts.

## Tech Stack
- **Database**: MySQL (Stores Transaction Status/Metadata)
- **Communication**: REST Code (FeignClient) to Account Service
- **Resilience**: Spring Retry

## Flow Diagrams

### Distributed Transaction Flow (Saga)
Reflects the `performTransaction` method logic.

```mermaid
sequenceDiagram
    participant Client
    participant TxService as Transaction Service
    participant TxDB as Transaction DB
    participant AccService as Account Service

    Client->>TxService: POST /api/transactions (type=TRANSFER)
    
    rect rgb(240, 248, 255)
    note right of TxService: Local Transaction 1
    TxService->>TxDB: Save Transaction (Status: PENDING)
    TxDB-->>TxService: txnId
    end

    rect rgb(255, 250, 240)
    note right of TxService: Remote Call (Retryable)
    TxService->>AccService: POST /api/accounts/transfer
    alt Success
        AccService-->>TxService: 200 OK
        
        rect rgb(240, 248, 255)
        note right of TxService: Local Transaction 2
        TxService->>TxDB: Update Status: SUCCESS
        end
        
        TxService-->>Client: 200 OK (SUCCESS)
    else Failure (e.g., Insufficient Funds)
        AccService-->>TxService: 400 Bad Request
        
        rect rgb(255, 230, 230)
        note right of TxService: Compensation / Update
        TxService->>TxDB: Update Status: FAILED
        end
        
        TxService-->>Client: 400 Bad Request (FAILED)
    end
    end
```
