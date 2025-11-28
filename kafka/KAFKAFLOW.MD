Excellent 👏 — since you’ve already implemented all the **microservices** (Auth, Customer, Account, Transaction, Card, Loan, Notification), let’s now clearly define the **Kafka producer → consumer event flow** across the entire Elevens Bank system.

We’ll go step-by-step through each service’s **Kafka behavior**, including:

* Which topics it **produces to**
* Which topics it **consumes from**
* What the **payloads** and **processing logic** look like
* And how the **end-to-end event flow** happens in the SAGA choreography

---

# 🌀 **Kafka Event Flow — Elevens Bank**

---

## 🧩 1. Auth Service → Customer Service

### 📤 Producer: Auth Service

**Event Type:** `UserCreatedEvent`
**Topic:** `bank.user.event.v1`

```java
// AuthService.java
public void registerUser(User user) {
    userRepository.save(user);

    UserCreatedEvent event = new UserCreatedEvent(
        user.getId(),
        user.getEmail(),
        user.getFullName(),
        LocalDateTime.now()
    );
    kafkaTemplate.send("bank.user.event.v1", event.getUserId(), event);
}
```

### 📥 Consumer: Customer Service

Consumes user creation event to create a corresponding customer profile.

```java
@KafkaListener(topics = "bank.user.event.v1", groupId = "customer-service-group")
public void consumeUserCreated(UserCreatedEvent event) {
    log.info("Received UserCreatedEvent: {}", event);
    Customer customer = new Customer(event.getUserId(), event.getFullName(), event.getEmail());
    customerRepository.save(customer);
}
```

✅ **Purpose:**
Keep `CustomerService` in sync with registered users.
Ensures eventual consistency via event-driven integration.

---

## 💰 2. Transaction Service → Account Service

### 📤 Producer: Transaction Service

**Event Type:** `DebitAccountCommand`
**Topic:** `bank.txn.command.v1`

```java
public void initiateWithdrawal(TransactionRequest req) {
    Transaction txn = transactionRepository.save(
        new Transaction(req.getCustomerId(), req.getAmount(), "PENDING")
    );

    DebitAccountCommand command = new DebitAccountCommand(
        txn.getTransactionId(), req.getAccountId(), req.getAmount()
    );

    kafkaTemplate.send("bank.txn.command.v1", command.getTransactionId(), command);
}
```

### 📥 Consumer: Account Service

Consumes the debit command, validates balance, and publishes a result event.

```java
@KafkaListener(topics = "bank.txn.command.v1", groupId = "account-service-group")
public void consumeDebitCommand(DebitAccountCommand command) {
    log.info("Processing DebitAccountCommand: {}", command);
    boolean success = accountService.debit(command.getAccountId(), command.getAmount());

    if (success) {
        kafkaTemplate.send("bank.txn.event.v1",
            command.getTransactionId(),
            new DebitSuccessfulEvent(command.getTransactionId(), command.getAmount()));
    } else {
        kafkaTemplate.send("bank.txn.event.v1",
            command.getTransactionId(),
            new DebitFailedEvent(command.getTransactionId(), "Insufficient funds"));
    }
}
```

✅ **Purpose:**
Implements **step 2** in the SAGA chain — the Account Service validates and executes the debit.

---

## 💳 3. Account Service → Transaction Service

### 📤 Producer: Account Service

Publishes `DebitSuccessfulEvent` or `DebitFailedEvent` to `bank.txn.event.v1`

### 📥 Consumer: Transaction Service

Consumes transaction outcomes and updates its local DB state.

```java
@KafkaListener(topics = "bank.txn.event.v1", groupId = "transaction-service-group")
public void consumeTransactionEvent(TransactionEvent event) {
    log.info("Received TransactionEvent: {}", event);

    transactionRepository.findById(event.getTransactionId())
        .ifPresent(txn -> {
            if (event instanceof DebitSuccessfulEvent) {
                txn.setStatus("SUCCESS");
                kafkaTemplate.send("bank.notification.event.v1", event);
            } else {
                txn.setStatus("FAILED");
                kafkaTemplate.send("bank.notification.event.v1", event);
            }
            transactionRepository.save(txn);
        });
}
```

✅ **Purpose:**
The Transaction Service **reacts to** Account Service results and continues the SAGA.

---

## 🔔 4. Transaction / Loan / Card Services → Notification Service

### 📤 Producer: Any Business Service

All business domain services can produce notification events to the **common stream**.

| Producer            | Topic                        | Event Example               |
| ------------------- | ---------------------------- | --------------------------- |
| Transaction Service | `bank.notification.event.v1` | `TransactionCompletedEvent` |
| Loan Service        | `bank.notification.event.v1` | `LoanApprovedEvent`         |
| Card Service        | `bank.notification.event.v1` | `CardActivatedEvent`        |

### 📥 Consumer: Notification Service

Consumes **all** notification events, processes them, and sends user notifications.

```java
@KafkaListener(topics = "bank.notification.event.v1", groupId = "notification-service-group")
public void consumeNotificationEvent(GenericEvent event) {
    log.info("Received Notification Event: {}", event);

    String message = switch (event.getEventType()) {
        case "TransactionCompletedEvent" -> "Your transaction is successful!";
        case "LoanApprovedEvent" -> "Your loan has been approved.";
        case "CardActivatedEvent" -> "Your card is now active.";
        default -> "Event notification received.";
    };

    emailService.sendEmail(event.getEmail(), "Elevens Bank Update", message);
}
```

✅ **Purpose:**
Centralized notification delivery with unified topic and simple event routing.

---

## 🧠 5. Loan Service → Account & Notification Service

### 📤 Producer: Loan Service

Publishes `LoanDisbursedEvent` or `LoanApprovedEvent` to `bank.loan.event.v1`

```java
kafkaTemplate.send("bank.loan.event.v1", new LoanDisbursedEvent(loanId, amount, customerId));
```

### 📥 Consumers:

* **Account Service** → credits the loan amount to account.
* **Notification Service** → sends alert about loan status.

```java
@KafkaListener(topics = "bank.loan.event.v1", groupId = "account-service-group")
public void handleLoanEvent(LoanDisbursedEvent event) {
    accountService.credit(event.getCustomerId(), event.getAmount());
}
```

---

## ⚙️ 6. Dead Letter Topics (DLTs)

Every consumer should have a **DLT** for error recovery.
For example, if the Account Service fails to process a debit:

| Main Topic                   | DLT Topic                        | Recovery Mechanism          |
| ---------------------------- | -------------------------------- | --------------------------- |
| `bank.txn.command.v1`        | `bank.txn.command.v1.DLT`        | Logged & retried manually   |
| `bank.txn.event.v1`          | `bank.txn.event.v1.DLT`          | Message moved for debugging |
| `bank.notification.event.v1` | `bank.notification.event.v1.DLT` | Fallback to log alerts      |

---

## 🔄 7. Complete SAGA Orchestration Summary

| Step | Service          | Action                             | Topic                        | Next Service     |
| ---- | ---------------- | ---------------------------------- | ---------------------------- | ---------------- |
| 1    | **Transaction**  | Publishes `DebitAccountCommand`    | `bank.txn.command.v1`        | **Account**      |
| 2    | **Account**      | Validates, debits, publishes event | `bank.txn.event.v1`          | **Transaction**  |
| 3    | **Transaction**  | Updates state, publishes result    | `bank.notification.event.v1` | **Notification** |
| 4    | **Notification** | Sends alerts (email/SMS)           | —                            | —                |

---

## 🧰 8. Configuration Snippet Example (Spring Boot)

### `application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: elevens-bank-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### Common Config Class

```java
@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

---

## ✅ End-to-End Summary

| Domain             | Producer → Topic → Consumer                         | Event Type                        | Outcome                 |
| ------------------ | --------------------------------------------------- | --------------------------------- | ----------------------- |
| User               | Auth → `bank.user.event.v1` → Customer              | `UserCreatedEvent`                | Customer created        |
| Transaction        | Transaction → `bank.txn.command.v1` → Account       | `DebitAccountCommand`             | Debit request           |
| Transaction Result | Account → `bank.txn.event.v1` → Transaction         | `DebitSuccessfulEvent`            | Update status           |
| Notifications      | Any → `bank.notification.event.v1` → Notification   | `TransactionCompletedEvent`, etc. | Notify user             |
| Loan               | Loan → `bank.loan.event.v1` → Account, Notification | `LoanDisbursedEvent`              | Credit account & notify |

---

Would you like me to generate a **Kafka flow diagram (Mermaid)** that visually connects all producers and consumers across these topics?
It’ll look like a **Kafka topology map** for your architecture — perfect for your documentation.
