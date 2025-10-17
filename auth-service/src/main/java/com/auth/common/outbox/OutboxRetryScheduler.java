package com.auth.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRetryScheduler {

    private final EventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000) // every 60 seconds -- every one minute
    public void retryPendingEvents() {
        List<EventOutbox> pendingEvents = outboxRepository.findByStatus("FAILED");

        for (EventOutbox event : pendingEvents) {
            try {
                Object payload = objectMapper.readValue(event.getPayload(), Object.class);
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), payload);
                event.setStatus("SENT");
                log.info("🔁 Retried and published outbox event id={} topic={}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.warn("⚠️ Retry failed for outbox id={} reason={}", event.getId(), e.getMessage());
            }
            event.setLastAttemptAt(java.time.LocalDateTime.now());
            outboxRepository.save(event);
        }
    }
}
