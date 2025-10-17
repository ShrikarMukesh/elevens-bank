package com.auth.kafka;

import com.auth.common.outbox.EventOutbox;
import com.auth.common.outbox.EventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void publishEvent(String topic, String key, Object event) {
        try {
            // 1️⃣ Save event in Outbox
            String payload = objectMapper.writeValueAsString(event);
            EventOutbox outbox = EventOutbox.builder()
                    .aggregateId(key)
                    .eventType(event.getClass().getSimpleName())
                    .topic(topic)
                    .payload(payload)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outbox);

            // 2️⃣ Try sending to Kafka asynchronously
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    RecordMetadata meta = result.getRecordMetadata();
                    log.info("✅ Event published to Kafka: topic={} offset={} key={}",
                            meta.topic(), meta.offset(), key);

                    outbox.setStatus("SENT");
                } else {
                    log.warn("⚠️ Failed to publish event to Kafka: {}", ex.getMessage());
                    outbox.setStatus("FAILED");
                }
                outbox.setLastAttemptAt(LocalDateTime.now());
                outboxRepository.save(outbox);
            });

        } catch (Exception e) {
            log.error("❌ Kafka unavailable — event saved in outbox. Error: {}", e.getMessage());
            saveFailedEvent(topic, key, event);
        }
    }

    private void saveFailedEvent(String topic, String key, Object event) {
        try {
            EventOutbox outbox = EventOutbox.builder()
                    .aggregateId(key)
                    .eventType(event.getClass().getSimpleName())
                    .topic(topic)
                    .payload(objectMapper.writeValueAsString(event))
                    .status("FAILED")
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("❌ Failed to save event to Outbox: {}", e.getMessage());
        }
    }
}
