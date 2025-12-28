# Customer Service

## Overview
The `customer-service` maintains customer profiles (KYC data), addresses, and contact information. It serves as the source of truth for user identity details beyond authentication credentials.

## Key Features
- **Profile Management**: CRUD operations for customer details.
- **KYC Verification**: Stores document references (simulated).

## Tech Stack
- **Database**: MySQL
- **Framework**: Spring Boot

## API Flow
```mermaid
graph LR
    Client --> API_Gateway
    API_Gateway -->|/api/customers/**| Customer_Service
    Customer_Service --> MySQL
```
