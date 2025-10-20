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

    @GetMapping("/customer/{customerId}")
    public List<Notification> getNotificationsByCustomerId(@PathVariable String customerId) {
        return notificationRepository.findByCustomerId(customerId);
    }

    // Get notification by ID
    @GetMapping("/{notificationId}")
    public ResponseEntity<Notification> getByNotificationId(@PathVariable String notificationId) {
        Optional<Notification> notification = notificationService.getNotificationById(notificationId);
        return notification.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PatchMapping("/customer/{customerId}/read-all")
    public ResponseEntity<List<Notification>> markAllAsRead(@PathVariable String customerId) {
        return ResponseEntity.ok(notificationService.markAllAsRead(customerId));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
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
