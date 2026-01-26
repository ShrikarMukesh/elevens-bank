# HashiCorp Vault Basics

## What is Vault?
HashiCorp Vault is a tool for securely accessing secrets. A valid secret is anything that you want to tightly control access to, such as API keys, passwords, or certificates. Vault provides a unified interface to any secret, while providing tight access control and recording a detailed audit log.

## Key Concepts

### 1. Secrets Engines
Vault has a modular architecture. "Secrets engines" are plugins that store, generate, or encrypt data.
- **KV (Key-Value)**: Simple storage for static secrets (e.g., database passwords). This is what we are using.
- **Database**: Generates dynamic, short-lived database credentials.
- **AWS**: Generates dynamic AWS access keys.

### 2. Authentication (Auth Methods)
Before you can access secrets, you must authenticate.
- **Token**: The core authentication method. Clients present a token to access Vault.
- **Kubernetes**: useful for services running in K8s.
- **AppRole**: Designed for machine-to-machine authentication (like your Spring Boot apps).

### 3. Paths
Secrets are stored at specific paths, like a filesystem.
- Example: `secret/data/cards-service`
    - `secret`: The mount point (the engine).
    - `data`: Required structure for KV version 2.
    - `cards-service`: The path for your specific application.

## Spring Cloud Vault Integration

Spring Cloud Vault allows your application to transparently fetch configuration properties from Vault.

### How it works:
1.  **Startup**: When your Spring Boot app starts, it connects to Vault.
2.  **Authentication**: It authenticates (using a token or AppRole).
3.  **Fetch**: It requests secrets from paths matching the application name and active profiles.
    - `secret/application` (Shared properties)
    - `secret/cards-service` (Service-specific properties)
    - `secret/cards-service/dev` (Profile-specific properties)
4.  **Property Source**: These secrets are added to the Spring Environment as a high-priority PropertySource, so they override values in `application.properties`.

### Configuration
In `application.properties`:
```properties
spring.config.import=vault://
spring.cloud.vault.uri=http://localhost:8200
spring.cloud.vault.authentication=token
spring.cloud.vault.token=root
```
*Note: In production, `spring.cloud.vault.token` should NEVER be hardcoded. It is usually injected via environment variables.*
