# Auth Service

## Overview
The `auth-service` handles user registration, authentication, and token management. It uses **JWT (JSON Web Tokens)** for stateless authentication across the microservices ecosystem. It also publishes events using **Kafka** upon user creation.

## Key Features
- **Registration**: Creates new users and hashes passwords using **BCrypt**.
- **Login**: Validates credentials and issues an Access Token (15m validity) and Refresh Token.
- **Token Refresh**: Rotates access tokens using a valid refresh token.
- **Event Publishing**: Publishes `UserCreatedEvent` to Kafka topic `bank.user.event.v1`.

## Tech Stack
- **Languages**: Java 21, Spring Boot 3
- **Security**: Spring Security, JWT (JJWT library)
- **Database**: MySQL (User/Role data), Redis (optional for session revocation)
- **Messaging**: Kafka

## Flow Diagrams

### 1. User Login Flow
```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant DB as MySQL Database
    
    Client->>AuthController: POST /auth/login (username, password)
    AuthController->>AuthService: login(username, password)
    AuthService->>DB: findByUsername()
    DB-->>AuthService: User Entity
    AuthService->>AuthService: validatePassword(BCrypt)
    
    alt Invalid Credentials
        AuthService-->>Client: 401 Unauthorized
    else Valid Credentials
        AuthService->>AuthService: generateTokens()
        AuthService->>DB: Save Refresh Token
        AuthService-->>AuthController: Tokens
        AuthController-->>Client: 200 OK (AccessToken, RefreshToken)
    end
```

### 2. Registration Flow
```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant DB as MySQL Database
    participant Kafka

    Client->>AuthController: POST /auth/register
    AuthController->>AuthService: register(User)
    AuthService->>DB: save(User)
    AuthService->>Kafka: publish(UserCreatedEvent)
    AuthService-->>Client: 201 Created
```
