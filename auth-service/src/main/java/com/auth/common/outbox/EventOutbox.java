package com.auth.common.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_outbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateId;
    private String eventType;
    private String topic;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status; // PENDING, SENT, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime lastAttemptAt;
}
