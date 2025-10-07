package com.notification.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String notificationId;
    private String eventSource;
    private String eventType;
    private String customerId;
    private String accountId;
    private String type;
    private String channel;
    private String priority;
    private String subject;
    private String message;
    private String status;
    private Instant sentAt;
    private int retryCount;
    private Map<String, Object> metadata;
    private Map<String, Object> audit;
}
