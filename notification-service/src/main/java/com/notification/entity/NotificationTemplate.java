package com.notification.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_templates")
public class NotificationTemplate {
    @Id
    private String id;
    private String templateId;
    private String eventType;
    private String channel;
    private String subject;
    private String message;
    private boolean isActive;
    private Instant createdAt;
}
