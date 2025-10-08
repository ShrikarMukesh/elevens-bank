package com.notification.controller;

import com.notification.dto.NotificationEvent;
import com.notification.entity.Notification;
import com.notification.service.NotificationService;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    // Get all notifications
    @GetMapping
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    // Get notification by ID
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getById(@PathVariable String id) {
        Optional<Notification> notification = notificationRepository.findById(id);
        return notification.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create notification via REST (like Kafka)
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationEvent event) {
        try {
            notificationService.processNotification(event);
            return ResponseEntity.ok("Notification processed successfully for eventType: " + event.getEventType());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to process notification: " + e.getMessage());
        }
    }

    // Delete a notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Delete all notifications
    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        notificationRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    /**
     * Send notification manually for testing EMAIL/SMS.
     */
    @PostMapping("/sendEmail")
    public ResponseEntity<String> sendNotificationEmail(@RequestBody NotificationEvent event) {
        log.info("🚀 Manual notification trigger received for eventType: {}", event.getEventType());
        try {
            notificationService.processNotification(event);
            return ResponseEntity.ok("✅ Notification processed successfully.");
        } catch (Exception e) {
            log.error("❌ Failed to process manual notification: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to process notification: " + e.getMessage());
        }
    }
}
