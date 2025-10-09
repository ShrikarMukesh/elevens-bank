🏦 Financial Microservices Architecture: Elevens Bank
1. Project Overview
   This repository contains the microservices architecture for "Elevens Bank," an event-driven system designed for scalability, high availability, and strong data consistency, primarily using the Spring Cloud ecosystem and Kafka as the central nervous system.

The architecture follows the C4 Model (Container View) to clearly define boundaries and communication.

2. Architecture Overview (C4 Container View)
   The system is composed of several independent services, each owning its specific data store and communicating primarily through the API Gateway (synchronous requests) and the Kafka Cluster (asynchronous events).

Container / Service

Role & Technology

Data Store Owner

Communication

Client Applications

Web, Mobile, or Internal Tools.

N/A

HTTP/REST (via API Gateway)

API Gateway

Spring Cloud Gateway

N/A

Routes all external traffic to internal services. Performs JWT validation.

Auth Service

Spring Boot (Java)

MySQL/PostgreSQL

Manages users, generates JWTs, publishes user-events to Kafka.

Customer Service

Spring Boot (Java)

MongoDB

Manages flexible customer profiles (KYC, preferences).

Account Service

Spring Boot (Java)

MySQL/PostgreSQL

Manages core account state, balance, and interest. Consumes txn-commands from Kafka.

Transaction Service

Spring Boot (Java)

MySQL/PostgreSQL

Initiates all fund movements. Publishes txn-commands to Kafka for processing.

Card Service

Spring Boot (Java)

MySQL/PostgreSQL

Manages card lifecycle, limits, and PIN hashes.

Loan Service

Spring Boot (Java)

MongoDB

Manages complex loan documents and repayment schedules.

Notification Service

Spring Boot (Java)

MongoDB

Consumes all event topics (e.g., txn-events, status-updates) and logs/delivers alerts.

Kafka Cluster

Confluent Platform

N/A

Asynchronous Communication Bus for SAGA transactions and event notifications.

Key Communication Flows
Request/Response (Synchronous): External calls flow through the API Gateway → Microservice (e.g., login, retrieve profile).

SAGA Pattern (Asynchronous): Used for complex, distributed transactions (e.g., withdrawals). Transaction Service → Kafka (txn-commands) → Account Service → Kafka (txn-events) → Transaction Service to ensure atomicity.

Event Notification: Any service publishes status updates → Kafka → Notification Service for user alerts and logging.

3. Implementation Roadmap (4 Weeks)
   The development is structured into four phases, building the foundation before layering on business logic.

Phase 1: Foundation and Identity (Week 1)
Service

Tasks

Key Deliverables

Infrastructure

Configure Eureka Discovery Service and Spring Cloud Gateway.

Dynamic routing for all services.

Auth Service

Implement users entity, repository, and basic Spring Security setup. Define user registration and login endpoints.

User registration and JWT generation.

Kafka Integration

Define all core Kafka topic names (user-events, txn-commands, txn-events).

Auth Service successfully publishes a UserCreated event.

Phase 2: Core Data Services (Week 2)
Service

Tasks

Key Deliverables

Customer Service

Define MongoDB entity for profiles. Implement endpoints to create and retrieve customer profile data.

Working Customer Profile CRUD operations.

Account Service

Define MySQL entity for accounts (including balance). Implement endpoints for opening new accounts. Map Kafka consumer for future use.

Accounts can be opened and retrieved by customer_id.

Inter-Service

Auth Service publishes UserCreated. Customer Service consumes user-events to create an initial profile.

Decoupled user/customer creation flow.

Phase 3: Value Transfer (Week 3)
Service

Tasks

Key Deliverables

Transaction Service

Implement the SAGA pattern: Produce DebitAccountCommand to Kafka. Consume DebitSuccessful/Failed events to update the transactions table status.

Full, robust Withdrawal transaction flow with state management.

Account Service

Implement transactional logic within a @Transactional boundary to safely debit/credit the account balance upon receiving a Kafka command.

ACID guarantees on balance updates.

Card Service

Implement cards entity. Define logic for checking daily/monthly limits before allowing a transaction to proceed.

Ability to link a card to an account and manage card status.

Phase 4: Extensions and User Feedback (Week 4)
Service

Tasks

Key Deliverables

Notification Service

Define MongoDB entity for notification logs. Implement Kafka listeners for all event topics (e.g., txn-events, card-status).

Centralized notification logging and processing pipeline.

Loan Service

Define MongoDB entity for loan documents. Implement complex application/disbursement logic. Publish loan-events to Kafka.

Loan application and disbursement logic integrated with Account Service credit flow.

Testing

Complete integration testing across all 7 services.

System ready for deployment and QA.

4. Local Development Setup
   Refer to the docker-compose.yml file for setting up the local Kafka and Zookeeper environment. Ensure your Spring Boot services use localhost:29092 for the Kafka bootstrap server.