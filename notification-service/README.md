# Notification Service

## Overview
The `notification-service` is responsible for sending alerts and notifications to users. It listens to events from other microservices (like `auth-service` or `transaction-service`) via **Kafka** and processes them asynchronously.

## Key Features
- **Event Driven**: Consumes messages from Kafka topics (e.g., `bank.user.event.v1`).
- **Async Processing**: Decouples email/SMS sending from the core transaction path.
- **Channels**: Supports Email, SMS, and Push Notifications (simulated).

## Tech Stack
- **Messaging**: Apache Kafka
- **Database**: MongoDB (NoSQL) for storing notification logs.

## Flow Diagrams

### Event Consumption Flow
```mermaid
sequenceDiagram
    participant Auth as Auth Service
    participant Kafka
    participant Notif as Notification Service
    participant Mongo as MongoDB
    participant SMTP as Email Server

    Auth->>Kafka: Publish UserCreatedEvent
    Kafka-->>Notif: Consume Message
    
    Notif->>Notif: Parse Event
    Notif->>Mongo: Save Notification Log (PENDING)
    
    Notif->>SMTP: Send Welcome Email
    SMTP-->>Notif: Success 200 OK
    
    Notif->>Mongo: Update Log (SENT)
```

## Error Handling & Reliability
### Dead Letter Topic (DLT)
To ensure **zero data loss**, this service implements a DLT strategy for failed Kafka messages.
- **Retry Policy**: 3 attempts with 1-second backoff.
- **DLT Topic**: `bank.notification.event.v1.DLT`

```mermaid
graph LR
    A[Kafka Topic] -- Consume --> B(Notification Consumer)
    B -- Success --> C[Processed]
    B -- Error --> D{Retry 3x}
    D -- Fail --> E[Publish to .DLT]
```
