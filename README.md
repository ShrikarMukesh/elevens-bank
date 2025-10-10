````markdown
# 🏦 Elevens Bank — Financial Microservices Architecture

> **Event-driven, Cloud-Native Banking Platform** built with Spring Boot, Spring Cloud, and Apache Kafka  
> Designed for scalability, high availability, and strong data consistency using **SAGA** and **CQRS** patterns.

---

## 📘 Table of Contents
- [1. Overview](#1-overview)
- [2. Architecture (C4 Container View)](#2-architecture-c4-container-view)
- [3. Key Communication Flows](#3-key-communication-flows)
- [4. SAGA Transaction Flow (Sequence Diagram)](#4-saga-transaction-flow-sequence-diagram)
- [5. Technology Stack](#5-technology-stack)
- [6. Implementation Roadmap (4 Weeks)](#6-implementation-roadmap-4-weeks)
- [7. Service Responsibilities](#7-service-responsibilities)
- [8. Kafka Topics](#8-kafka-topics)
- [9. Getting Started](#9-getting-started)
- [10. Future Enhancements](#10-future-enhancements)
- [11. License](#11-license)

---

## 1. Overview

**Elevens Bank** is a modular, microservices-based financial system implementing **event-driven architecture** with **Spring Cloud** and **Apache Kafka**.  
Each service owns its database, ensuring **strong data consistency**, **fault isolation**, and **scalability**.  

It supports:
- ✅ JWT-based authentication & authorization  
- ✅ SAGA orchestration for distributed transactions  
- ✅ Reactive event streaming with Kafka  
- ✅ Centralized logging & notification delivery  

---

## 2. Architecture (C4 Container View)

### 🧩 System Overview Diagram (C4 Container View)

```mermaid
flowchart TB
    subgraph Clients[Client Applications]
        WebApp[Web App]
        MobileApp[Mobile App]
    end

    Clients -->|HTTP/REST| APIGateway

    subgraph Core[Core Microservices]
        APIGateway[API Gateway\n(Spring Cloud Gateway)]
        AuthService[Auth Service\n(Spring Boot + MySQL/PostgreSQL)]
        CustomerService[Customer Service\n(Spring Boot + MongoDB)]
        AccountService[Account Service\n(Spring Boot + MySQL/PostgreSQL)]
        TransactionService[Transaction Service\n(Spring Boot + MySQL/PostgreSQL)]
        CardService[Card Service\n(Spring Boot + MySQL/PostgreSQL)]
        LoanService[Loan Service\n(Spring Boot + MongoDB)]
        NotificationService[Notification Service\n(Spring Boot + MongoDB)]
    end

    subgraph Infra[Infrastructure]
        Eureka[Eureka Discovery Service]
        Kafka[Kafka Cluster\n(Confluent Platform)]
    end

    APIGateway --> AuthService
    APIGateway --> CustomerService
    APIGateway --> AccountService
    APIGateway --> TransactionService
    APIGateway --> CardService
    APIGateway --> LoanService
    APIGateway --> NotificationService

    AuthService -->|Publishes user-events| Kafka
    TransactionService -->|Publishes txn-commands| Kafka
    AccountService -->|Publishes txn-events| Kafka
    LoanService -->|Publishes loan-events| Kafka
    CardService -->|Publishes card-status| Kafka

    Kafka -->|Consumes events| NotificationService
    Kafka -->|Consumes user-events| CustomerService
    Kafka -->|Consumes txn-commands| AccountService
    Kafka -->|Consumes txn-events| TransactionService

    Eureka -.-> Core
````

---

## 3. Key Communication Flows

| Type                     | Description                                                     | Example                                     |
| ------------------------ | --------------------------------------------------------------- | ------------------------------------------- |
| **Synchronous (HTTP)**   | Through API Gateway for direct requests                         | `GET /customers/{id}`                       |
| **Asynchronous (Kafka)** | For distributed transactions & events                           | `txn-commands`, `txn-events`, `user-events` |
| **SAGA Pattern**         | Transaction coordination using event choreography               | Withdrawals, transfers                      |
| **Event Notification**   | Any service publishes an event → Notification Service processes | `txn-success`, `loan-approved`              |

---

## 4. SAGA Transaction Flow (Sequence Diagram)

### 🔁 Withdrawal Example (SAGA Pattern via Kafka)

```mermaid
sequenceDiagram
    participant User
    participant TransactionService
    participant Kafka
    participant AccountService
    participant NotificationService

    User->>TransactionService: POST /transactions/withdraw (amount=₹500)
    TransactionService->>Kafka: Publish DebitAccountCommand (txnId=TXN123)
    
    Kafka->>AccountService: Consume DebitAccountCommand
    AccountService->>AccountService: Verify balance & debit account
    
    alt Debit Successful
        AccountService->>Kafka: Publish DebitSuccessfulEvent (txnId=TXN123)
        Kafka->>TransactionService: Consume DebitSuccessfulEvent
        TransactionService->>TransactionService: Update txn_status=SUCCESS
        TransactionService->>Kafka: Publish TransactionCompletedEvent
        Kafka->>NotificationService: Consume TransactionCompletedEvent
        NotificationService->>NotificationService: Send email/SMS to user
    else Debit Failed
        AccountService->>Kafka: Publish DebitFailedEvent (txnId=TXN123)
        Kafka->>TransactionService: Consume DebitFailedEvent
        TransactionService->>TransactionService: Update txn_status=FAILED
        TransactionService->>Kafka: Publish TransactionFailedEvent
        Kafka->>NotificationService: Consume TransactionFailedEvent
        NotificationService->>NotificationService: Notify user of failure
    end
```

📘 **Summary:**

* Transaction Service **initiates** a debit request by publishing `DebitAccountCommand`.
* Account Service **validates** balance and publishes either `DebitSuccessfulEvent` or `DebitFailedEvent`.
* Transaction Service **updates** transaction status and publishes a result event.
* Notification Service **listens** to all transaction outcomes and alerts the customer.

---

## 5. Technology Stack

| Layer                            | Technologies                                                      |
| -------------------------------- | ----------------------------------------------------------------- |
| **Backend Framework**            | Spring Boot 3.x, Spring WebFlux, Spring Data JPA, Spring Security |
| **Microservices Infrastructure** | Spring Cloud Gateway, Eureka Discovery, Config Server             |
| **Event Streaming**              | Apache Kafka / Confluent Platform                                 |
| **Databases**                    | MySQL / PostgreSQL, MongoDB                                       |
| **Authentication**               | JWT, OAuth2                                                       |
| **Containerization**             | Docker, Kubernetes (optional)                                     |
| **Build & CI/CD**                | Maven, Jenkins / GitHub Actions                                   |
| **Monitoring**                   | ELK Stack, Prometheus, Grafana                                    |

---

## 6. Implementation Roadmap (4 Weeks)

| Phase                              | Week   | Services                   | Key Deliverables                                           |
| ---------------------------------- | ------ | -------------------------- | ---------------------------------------------------------- |
| **Phase 1: Foundation & Identity** | Week 1 | Gateway, Eureka, Auth      | Dynamic routing, JWT generation, publish `UserCreated`     |
| **Phase 2: Core Data Services**    | Week 2 | Customer, Account          | CRUD operations, consume `user-events`                     |
| **Phase 3: Value Transfer**        | Week 3 | Transaction, Account, Card | SAGA flow for withdrawals, card validation                 |
| **Phase 4: Extensions & Feedback** | Week 4 | Loan, Notification         | Loan logic, centralized notifications, integration testing |

---

## 7. Service Responsibilities

| Service                  | Key Responsibilities                                                    |
| ------------------------ | ----------------------------------------------------------------------- |
| **Auth Service**         | Manage users, login/signup, JWT token generation, publish `UserCreated` |
| **Customer Service**     | Manage customer KYC, preferences, consume `UserCreated`                 |
| **Account Service**      | Manage balances, debit/credit operations, consume `txn-commands`        |
| **Transaction Service**  | Initiate fund transfers, manage transaction states, publish events      |
| **Card Service**         | Manage cards, limits, and lifecycle                                     |
| **Loan Service**         | Manage loan documents, disbursements, and repayments                    |
| **Notification Service** | Consume all events, log, and send notifications via email/SMS           |

---

## 8. Kafka Topics

| Topic Name            | Producer            | Consumer                  | Description                  |
| --------------------- | ------------------- | ------------------------- | ---------------------------- |
| `user-events`         | Auth Service        | Customer Service          | User profile creation event  |
| `txn-commands`        | Transaction Service | Account Service           | Command to debit/credit      |
| `txn-events`          | Account Service     | Transaction, Notification | Transaction result           |
| `loan-events`         | Loan Service        | Account, Notification     | Loan disbursement updates    |
| `card-status`         | Card Service        | Notification              | Card activation/deactivation |
| `notification-events` | All Services        | Notification              | Unified alert stream         |

---

## 9. Getting Started

### 🧱 Prerequisites

* Java 17+
* Maven 3.9+
* Docker & Docker Compose
* Kafka & Zookeeper (via Confluent or Docker)

### 🚀 Setup Steps

```bash
# 1. Clone the repository
git clone https://github.com/<your-org>/elevens-bank.git
cd elevens-bank

# 2. Start Kafka & Zookeeper
docker-compose up -d kafka zookeeper

# 3. Start Eureka and API Gateway
cd eureka-server && mvn spring-boot:run
cd ../api-gateway && mvn spring-boot:run

# 4. Run each service
cd ../auth-service && mvn spring-boot:run
cd ../customer-service && mvn spring-boot:run
# ...repeat for all services
```

### 🧪 Test Endpoints

```bash
# Register a user
POST /auth/register
{
  "email": "john.doe@example.com",
  "password": "password123"
}

# Open a new account
POST /accounts
{
  "customerId": "CUST1001",
  "initialBalance": 5000
}
```

---

## 10. Future Enhancements

* Integrate **Circuit Breaker (Resilience4j)** for fault tolerance
* Implement **API Rate Limiting** in API Gateway
* Add **Audit Logging Service** for compliance
* Enable **OpenTelemetry tracing** for distributed monitoring

---

## 11. License

This project is licensed under the **MIT License** — feel free to use, modify, and distribute with attribution.

---

### 👨‍💻 Author

**Shrikar**
Senior Java Developer • Cognizant
Expert in Spring Boot, Kafka, Microservices, and Cloud-Native Architectures
📧 *[[your.email@example.com](mailto:your.email@example.com)]*

```


