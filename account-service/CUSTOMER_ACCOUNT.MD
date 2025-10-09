Perfect 💪 — this is where your **Accounts microservice** becomes “event-driven.”
It will **listen to Kafka events from Customer Service** (`customer.events`) and automatically react — e.g., create a **default Savings Account** when a customer’s KYC is verified.

---

## 🧩 **1️⃣ Add Dependencies (in Accounts Service `pom.xml`)**

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## ⚙️ **2️⃣ Kafka Consumer Configuration**

📁 `com.accounts.config.KafkaConsumerConfig.java`

```java
package com.accounts.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>();
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "account-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

---

## 🧾 **3️⃣ Create Customer Event DTO**

📁 `com.accounts.event.CustomerEvent.java`

```java
package com.accounts.event;

import lombok.Data;

import java.time.Instant;

@Data
public class CustomerEvent {
    private String eventType;    // CUSTOMER_CREATED, CUSTOMER_VERIFIED
    private String customerId;
    private String userId;
    private boolean verified;
    private Instant verifiedAt;
}
```

---

## 🧠 **4️⃣ Kafka Consumer Implementation**

📁 `com.accounts.kafka.CustomerEventConsumer.java`

```java
package com.accounts.kafka;

import com.accounts.event.CustomerEvent;
import com.accounts.entity.Account;
import com.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerEventConsumer {

    private final AccountRepository accountRepository;

    @KafkaListener(topics = "customer.events", groupId = "account-service")
    public void consume(CustomerEvent event) {
        log.info("Received Kafka Event: {}", event);

        try {
            switch (event.getEventType()) {
                case "CUSTOMER_VERIFIED":
                    handleCustomerVerified(event);
                    break;
                case "CUSTOMER_CREATED":
                    log.info("Customer created event received (no account yet): {}", event.getCustomerId());
                    break;
                default:
                    log.warn("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception ex) {
            log.error("Error processing event: {}", event, ex);
        }
    }

    private void handleCustomerVerified(CustomerEvent event) {
        // Check if account already exists for this customer
        boolean exists = accountRepository.existsByCustomerId(event.getCustomerId());
        if (exists) {
            log.info("Account already exists for customer {}", event.getCustomerId());
            return;
        }

        // Create default Savings Account
        Account account = new Account();
        account.setCustomerId(Long.parseLong(event.getCustomerId().replaceAll("\\D", ""))); // numeric extraction
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType("SAVINGS");
        account.setBalance(0.0);
        account.setCurrency("INR");
        account.setStatus("ACTIVE");
        account.setCreatedAt(Instant.now());

        accountRepository.save(account);
        log.info("✅ Default savings account created for verified customer: {}", event.getCustomerId());
    }

    private String generateAccountNumber() {
        return "AC" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
```

✅ **Consumes from** `customer.events`
✅ **Group ID** = `account-service` (each microservice can have its own group)
✅ **Creates an account** only if customer verified and not already existing

---

## 🧱 **5️⃣ Account Repository (example)**

📁 `com.accounts.repository.AccountRepository.java`

```java
package com.accounts.repository;

import com.accounts.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByCustomerId(Long customerId);
}
```

---

## 🧩 **6️⃣ Optional: Retry & Dead Letter Setup (for resilience)**

To prevent message loss:

```yaml
spring:
  kafka:
    listener:
      ack-mode: RECORD
      concurrency: 3
      retry:
        max-attempts: 3
      error-handler:
        dead-letter-topic: customer.events.dlt
```

When processing fails after retries → the event goes to **`customer.events.dlt`** for later inspection.

---

## 🧠 **7️⃣ How the Flow Works**

| Step | Service              | Action                                            |
| ---- | -------------------- | ------------------------------------------------- |
| 1️⃣  | **Customer Service** | Emits `CUSTOMER_VERIFIED` event on successful KYC |
| 2️⃣  | **Kafka Broker**     | Broadcasts event to `customer.events` topic       |
| 3️⃣  | **Accounts Service** | Listens, consumes event, checks if account exists |
| 4️⃣  | **If none found**    | Creates a new Savings Account automatically       |

---

Perfect! Let’s make your **Kafka consumer production-ready** with **retry**, **Dead Letter Topic (DLT)**, and **Prometheus metrics**. I’ll break it down step by step.

---

## 1️⃣ **Add Spring Kafka and Micrometer Dependencies**

`pom.xml`:

```xml
<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Micrometer for Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## 2️⃣ **Kafka Configuration with Retry and DLT**

📁 `KafkaConsumerConfig.java`

```java
package com.accounts.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.common.TopicPartition;
import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>();
        valueDeserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "account-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, valueDeserializer);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Dead Letter Publishing Recoverer (sends failed messages to DLT)
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate, 
                    (record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition()));

        // Retry configuration: 3 attempts with 2s backoff
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, 
                new org.springframework.util.backoff.FixedBackOff(2000L, 3));

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
```

✅ **Explanation:**

* **Retry:** 3 attempts, 2 seconds apart.
* **DLT:** Failed events after retries are sent to `<topic>.dlt` (e.g., `customer.events.dlt`).
* **ErrorHandlingDeserializer:** avoids consumer crashing due to serialization errors.

---

## 3️⃣ **Account Service Kafka Consumer**

Update `CustomerEventConsumer.java` to track metrics:

```java
package com.accounts.kafka;

import com.accounts.event.CustomerEvent;
import com.accounts.entity.Account;
import com.accounts.repository.AccountRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerEventConsumer {

    private final AccountRepository accountRepository;
    private final Counter eventCounter;
    private final Counter failedEventCounter;

    public CustomerEventConsumer(AccountRepository accountRepository, MeterRegistry registry) {
        this.accountRepository = accountRepository;
        this.eventCounter = registry.counter("kafka.events.processed", "topic", "customer.events");
        this.failedEventCounter = registry.counter("kafka.events.failed", "topic", "customer.events");
    }

    @KafkaListener(topics = "customer.events", groupId = "account-service")
    public void consume(CustomerEvent event) {
        try {
            eventCounter.increment();
            log.info("Received Kafka Event: {}", event);

            if ("CUSTOMER_VERIFIED".equals(event.getEventType())) {
                handleCustomerVerified(event);
            }
        } catch (Exception ex) {
            failedEventCounter.increment();
            log.error("Error processing event: {}", event, ex);
            throw ex; // trigger retry/DLT
        }
    }

    private void handleCustomerVerified(CustomerEvent event) {
        boolean exists = accountRepository.existsByCustomerId(Long.parseLong(event.getCustomerId().replaceAll("\\D", "")));
        if (exists) {
            log.info("Account already exists for customer {}", event.getCustomerId());
            return;
        }

        Account account = new Account();
        account.setCustomerId(Long.parseLong(event.getCustomerId().replaceAll("\\D", "")));
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType("SAVINGS");
        account.setBalance(0.0);
        account.setCurrency("INR");
        account.setStatus("ACTIVE");
        account.setCreatedAt(Instant.now());

        accountRepository.save(account);
        log.info("✅ Default savings account created for verified customer: {}", event.getCustomerId());
    }

    private String generateAccountNumber() {
        return "AC" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
```

---

## 4️⃣ **Prometheus Metrics Integration**

Spring Boot Actuator + Micrometer expose metrics automatically:

`application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    enable:
      all: true
```

* Visit `http://localhost:8080/actuator/prometheus` to see:

    * `kafka_events_processed_total`
    * `kafka_events_failed_total`
* Prometheus can scrape these metrics for alerting.

---

## ✅ **What We Achieve Now**

| Feature                       | Benefit                                      |
| ----------------------------- | -------------------------------------------- |
| Retry on consumer failure     | Avoids transient issues breaking the flow    |
| Dead Letter Topic             | Failed events saved for later inspection     |
| Prometheus counters           | Track total processed & failed events        |
| Event-driven account creation | Accounts auto-created for verified customers |

---

If you want, I can create a **full end-to-end diagram** showing **Customer Service → Kafka → Account Service** with **DLT and metrics monitoring**, so it’s crystal clear for your architecture docs.

Do you want me to do that next?
