package com.customers.kafka;

import com.customers.event.CustomerEvent;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerEventProducer {

    @Value("${spring.kafka.topic.customer-events}")
    private String customerTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCustomerEvent(CustomerEvent event) {
        int attempts = 0;
        boolean sent = false;

        while (!sent && attempts < 3) {
            try {
                kafkaTemplate.send(customerTopic, event.customerId(), event).get(); // synchronous send
                sent = true;
                log.info("CustomerEvent sent: {}", event.customerId());
            } catch (Exception ex) {
                attempts++;
                log.warn("Attempt {} failed to send CustomerEvent: {}", attempts, ex.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }

        if (!sent) {
            log.error("Failed to send CustomerEvent after 3 attempts: {}", event.customerId());
        }
    }

}
