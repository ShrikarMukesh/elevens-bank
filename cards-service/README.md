# Cards Service

## Overview
The `cards-service` manages Debit and Credit cards issued to customers. It handles card issuance, blocking, and limit management.

## Key Features
- **Card Issuance**: Generate new cards for accounts.
- **Card Management**: Block/Unblock cards.
- **Limit Checks**: Verify if a transaction exceeds card limits.

## Tech Stack
- **Database**: MySQL
- **Framework**: Spring Boot

## API Flow
```mermaid
graph LR
    Client --> API_Gateway
    API_Gateway -->|/cards/**| Cards_Service
    Cards_Service --> MySQL
```
