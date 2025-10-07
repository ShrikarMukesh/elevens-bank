package com.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.dto.NotificationEvent;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper; // <-- Injected by Spring Boot

    @KafkaListener(topics = "${spring.kafka.topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            log.info("📥 Received Kafka Event: {}", event);
            notificationService.processNotification(event);
        } catch (Exception e) {
            log.error("❌ Failed to process Kafka message: {}", message, e);
        }
    }
}
