# API Gateway

## Overview
The `api-gateway` is the single entry point for all client requests. It intelligently routes traffic to the appropriate microservices while providing cross-cutting concerns like security, rate limiting, and observability.

## Key Features
- **Dynamic Routing**: Uses **Spring Cloud Gateway** to route requests based on paths (e.g., `/auth/**` -> `auth-service`).
- **Service Discovery**: Integrates with **Eureka** to resolve service locations dynamically (`lb://service-name`).
- **Rate Limiting**: Implements **Token Bucket Algorithm** using Redis to prevent abuse (e.g., 10 req/s per IP).
- **Security**: Validates JWT tokens for protected routes.
- **Circuit Breaker**: Resilience4j integration to fail fast if downstream services are down.

## Tech Stack
- **Framework**: Spring Cloud Gateway (Reactive)
- **Discovery**: Netflix Eureka Client
- **Rate Limiting**: Redis Reactive Rate Limiter
- **Tracing**: Zipkin/Sleuth

## Flow Diagrams

### Request Routing Flow
```mermaid
graph LR
    Client[Client Request] -->|Host/Path| Gateway[API Gateway]
    
    subgraph Gateway Logic
        Filter1[Rate Limiter] --> Filter2[Auth Filter]
        Filter2 --> Router{Route Matcher}
    end
    
    Gateway --> Filter1
    
    Router -->|/auth/**| Auth[Auth Service]
    Router -->|/api/accounts/**| Account[Account Service]
    Router -->|/api/transactions/**| Tx[Transaction Service]
    
    Auth --> Eureka[Eureka Registry]
    Account --> Eureka
```

### Rate Limiting Logic (Token Bucket)
```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Redis

    Client->>Gateway: GET /api/accounts/123
    Gateway->>Redis: Request Tokens for IP (1.2.3.4)
    
    alt Tokens Available
        Redis-->>Gateway: Allow (Rem: 9)
        Gateway->>AccountService: Forward Request
        AccountService-->>Gateway: Response
        Gateway-->>Client: 200 OK
    else Tokens Exhausted
        Redis-->>Gateway: Deny
        Gateway-->>Client: 429 Too Many Requests
    end
```
