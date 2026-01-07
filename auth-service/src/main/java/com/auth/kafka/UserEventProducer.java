package com.auth.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserEventProducer {

    @Value("${spring.kafka.template.default-topic}")
    private String userEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreated(String email) {
        Map<String, Object> event = Map.of(
                "eventType", "UserCreated",
                "email", email,
                "timestamp", System.currentTimeMillis()
        );
        kafkaTemplate.send(userEventsTopic, event);
    }
}
