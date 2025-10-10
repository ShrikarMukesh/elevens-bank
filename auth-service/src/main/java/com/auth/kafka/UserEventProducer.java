package com.auth.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreated(String email) {
        Map<String, Object> event = Map.of(
                "eventType", "UserCreated",
                "email", email,
                "timestamp", System.currentTimeMillis()
        );
        kafkaTemplate.send("user-events", event);
    }
}
