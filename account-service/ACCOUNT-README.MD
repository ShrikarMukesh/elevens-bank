---

# 🏦 Banking Account Service – Spring Boot Microservice

### Clean Architecture • SOLID Principles • Spring Security (JWT) • JPA • MySQL

---

## 📌 Overview

This project is a **Banking Account Microservice** built using **Spring Boot, Spring Security (JWT), JPA, MySQL**, and follows a clean, layered architecture guided heavily by **SOLID design principles**.

The service supports:

* Create Account
* Deposit, Withdraw
* Transfer (Atomic + Locked Row)
* Fetch Accounts
* JWT-based Authentication
* Clean separation into Controller → Service → Repository layers

---

# 🧱 **Architecture Diagram**

```
┌────────────────────────┐
│   AccountController    │  ← Handles HTTP & Security Annotations
└───────────┬────────────┘
            │  (Delegates)
┌───────────▼────────────┐
│   AccountServiceImpl    │  ← Business Logic Layer (SRP)
│   (Implements Interface)│
└───────────┬────────────┘
            │  (Depends on Abstraction)
┌───────────▼────────────┐
│   AccountRepository     │  ← Persistence Layer (JPA)
└───────────┬────────────┘
            │
        MySQL DB
```

---

# 🧩 **SOLID Principle Implementation**

This project is intentionally structured around SOLID principles.

---

## **S — Single Responsibility Principle**

Each layer has exactly one responsibility:

### ✔ Controllers

* Handle HTTP routing, logging, request/response creation
* No business logic inside

### ✔ Services

* Contain domain logic: create account, withdrawals, transfers
* Use `@Transactional` for atomic operations

### ✔ Repositories

* Database operations only (findById, findByCustomerId, pessimistic locking)

### ✔ Security Layer

* `SecurityConfig` → only security configuration
* `JwtAuthenticationFilter` → only token validation and authentication

➡ **Result:** Easy to test, understand, and extend.

---

## **O — Open/Closed Principle**

* Add new endpoints → no modification to existing ones
* Extend service logic without changing existing methods
* Add new security rules without modifying filter logic
* Add new repository queries without touching existing ones

➡ **Result:** Feature additions don’t break existing code.

---

## **L — Liskov Substitution Principle**

* `AccountService` interface ensures substitutable implementations
* Mock services can be injected for testing
* Spring Security accepts any Authentication implementation (LSP-friendly)

➡ **Result:** Better flexibility and testability.

---

## **I — Interface Segregation Principle**

* `AccountService` only contains account-related operations
* `AccountRepository` only contains persistence operations
* No “God interfaces”

➡ **Result:** Interfaces stay small, focused, and easy to maintain.

---

## **D — Dependency Inversion Principle**

* Controllers depend on `AccountService` abstraction
* Services depend on `AccountRepository` interface
* JWT filter depends on `JwtUtil` abstraction
* Spring Boot DI manages dependencies via constructor injection

➡ **Result:** Loose coupling, high maintainability.

---

| Method | Endpoint                              | Description          |
| ------ | ------------------------------------- | -------------------- |
| GET    | `/api/accounts/status`                | Service health check |
| POST   | `/api/accounts/create`                | Create new account   |
| GET    | `/api/accounts/{id}`                  | Fetch account by ID  |
| GET    | `/api/accounts/customer/{customerId}` | Accounts by customer |
| GET    | `/api/accounts`                       | Fetch all accounts   |
| POST   | `/api/accounts/{id}/deposit`          | Deposit amount       |
| POST   | `/api/accounts/{id}/withdraw`         | Withdraw amount      |
| POST   | `/api/accounts/transfer`              | Transfer money       |

---
# 🔒 Security Architecture

### **JWT Authentication Flow**

1. Client sends `Authorization: Bearer <token>`
2. `JwtAuthenticationFilter` validates token
3. Extracts user + roles
4. Sets authentication in `SecurityContextHolder`
5. Request moves forward as an authenticated call

Security is modular, testable, and fully decoupled from business logic.

---

# 🚀 Tech Stack

* **Java 17**
* **Spring Boot**
* **Spring Security (JWT)**
* **Spring Data JPA (Hibernate)**
* **MySQL**
* **Lombok**
* **Maven**

---

# 📁 Folder Structure

```
src/main/java/com/accounts
│
├── controller       → API endpoints (SRP)
├── service          → Business logic layer
│   ├── impl         → Concrete implementations
│   └── AccountService.java
├── repository       → JPA interfaces
├── entity           → Database models (Account)
├── security         → JWT filter + utilities
└── config           → Security configuration
```

---

# 🧪 Testing Notes

* Services are unit tested using mock repositories
* JWT logic tested independently via JwtUtil
* Controller tests use MockMvc or WebTestClient

---

# 🏁 Running the Project

### **1. Configure MySQL**

```sql
CREATE DATABASE accounts_db;
```

### **2. Update application.properties**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/accounts_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### **3. Run Application**

```bash
mvn spring-boot:run
```

---

# ⭐ Final Notes

This project is designed with:

* **Clean separation of layers**
* **High maintainability**
* **Extensibility**
* **Testability**
* **Industry-standard solid architecture**

---

| Category            | Benefit                                              |
| ------------------- | ---------------------------------------------------- |
| ⚙️ Data Consistency | `customerId` aligned with Customer Service           |
| 💾 Data Safety      | Optimistic locking + audit fields                    |
| 📊 Query Speed      | Indexed on common fields                             |
| 🧩 Integration      | `userId` supports Auth → Account linkage             |
| 💸 Banking Realism  | Tracks available balance + lifecycle dates           |
| 🔐 Security         | Ready for JPA Auditing (Spring Security integration) |


Here is a **clean, complete, interview-ready summary** of **ALL SOLID principles used in your project**, based on the classes you shared.

You can literally speak this summary in your interview.
This covers **Controller + Service + Repository + Security + Filters + Interfaces**.

---

# ✅ **FULL SOLID SUMMARY (BASED ON YOUR CODE)**

### ⭐ Interview-Ready | Clear | Professional | Specific to Your Code

---

# **S — Single Responsibility Principle**

### ✔ You applied SRP across all layers:

### **1. Controller → Only handles HTTP API concerns**

* Logging
* Request mapping (`@GetMapping`, `@PostMapping`, etc.)
* Passing data to service
* Returning `ResponseEntity`

**Business logic is never inside the controller.**
➡️ Example:

```java
accountServiceImpl.deposit(id, amount);
```

---

### **2. Service Layer → Only contains business logic**

Every method handles one business responsibility:

* `createAccount()` → account creation
* `withdraw()` → withdrawal rules
* `deposit()` → deposit
* `transfer()` → transactional funds movement

No HTTP logic, no security logic.

---

### **3. Repository Layer → Only does database operations**

```java
findByCustomerId()
findByAccountNumber()
findByIdForUpdate()
```

Every method is responsible for **exactly one query**.

---

### **4. SecurityConfig → Only configures security**

* CORS rules
* CSRF
* URL authorization rules
* Security filter chain

No business logic or authentication logic inside.

---

### **5. JwtAuthenticationFilter → Only authenticates the request**

* Extract token
* Validate token
* Set authentication

Does NOT generate tokens or handle authorization.

---

✔ **SRP is the strongest SOLID principle you applied.**

---

# **O — Open/Closed Principle**

Your classes are **open for extension** but **closed for modification**:

### ✔ Adding new endpoints does NOT require modifying existing controller methods

Each API (deposit, transfer, withdraw, etc.) is isolated.

### ✔ Security rules can be extended without modifying internal filter logic

You can add new permitted URLs in `SecurityConfig` without editing `JwtAuthenticationFilter`.

### ✔ New business features can be added by creating new service methods

No need to modify existing business logic.

---

# **L — Liskov Substitution Principle**

### ✔ Applied in your service & filter:

### **1. `AccountService` interface**

Any implementation (`AccountServiceImpl`, mock service, future service)
can **replace** the current one without breaking the controller.

### **2. `Authentication` objects**

Spring Security can substitute:

* `UsernamePasswordAuthenticationToken`
* `JwtAuthenticationToken`
* `OAuth2AuthenticationToken`

Your filter still works → follows LSP.

---

# **I — Interface Segregation Principle**

### ✔ Applied in your design:

### **1. `AccountService` interface**

Focused only on **account operations**.
It does NOT force:

* Loan operations
* Customer operations
* Transaction history operations

Controllers depend on **only what they need**.

### ✔ Repository interface is small and focused

`AccountRepository` has only account-related methods.

---

# **D — Dependency Inversion Principle**

### ✔ You apply DIP very well using Spring IOC:

### **1. Controller depends on `AccountService` abstraction**

(Not fully applied because you injected the impl—easy fix)

### **2. Service depends on `AccountRepository` interface**

Not JDBC, not SQL — but repository abstraction.

### **3. JwtAuthenticationFilter depends on `JwtUtil`**

JWT logic is injected, not hardcoded.

### **4. SecurityConfig exposes `SecurityFilterChain` as a bean**

Other layers depend on abstractions, not implementations.

---

# 🎯 FINAL INTERVIEW SUMMARY (Use This Version)

> “My project follows SOLID principles very well.
> The controller layer follows **SRP** by focusing only on HTTP requests while delegating business logic to the service.
> The service layer is also SRP-compliant since each method handles one business capability.
> The repository follows **ISP** and SRP with small focused DB methods.
> Through interfaces like `AccountService`, the system follows **DIP** and **LSP**, allowing any implementation to be substituted without breaking controllers.
> The system is **OCP-compliant** because new features can be added without modifying existing classes—new endpoints, new services, new queries, all extend the system cleanly.
> Security also follows SOLID: `SecurityConfig` handles only configuration, and `JwtAuthenticationFilter` handles only authentication.
> Overall, the architecture cleanly demonstrates all SOLID principles.”

---
