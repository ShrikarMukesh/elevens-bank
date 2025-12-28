# Discovery Service (Eureka)

## Overview
The `discovery-service` is the Service Registry for the Elevens Bank microservices ecosystem. It uses **Netflix Eureka** to allow services to register themselves and discover other services dynamically without hardcoded IP addresses.

## Key Features
- **Service Registration**: Services register their Host and Port on startup.
- **Heartbeats**: Services send periodic heartbeats (default 30s) to renew their lease.
- **Client-Side Load Balancing**: Clients (like API Gateway) fetch the registry and load balance requests locally.

## Tech Stack
- **Framework**: Spring Cloud Netflix Eureka Server

## Flow Diagrams

### Service Registration & Discovery
```mermaid
sequenceDiagram
    participant Auth as Auth Service
    participant Eureka as Discovery Server
    participant Gateway as API Gateway

    Note over Auth: Startup
    Auth->>Eureka: Register (Name: AUTH-SERVICE, IP: 10.0.0.5)
    
    Note over Gateway: Startup / Refresh
    Gateway->>Eureka: Fetch Registry
    Eureka-->>Gateway: List [AUTH-SERVICE: 10.0.0.5]
    
    Gateway->>Auth: Forward Request (http://10.0.0.5/login)
```
