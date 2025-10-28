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

    @GetMapping
    public List<Notification> getAll() {
        log.info("GET /api/notifications - fetching all notifications");
        List<Notification> notifications = notificationRepository.findAll();
        log.info("Found {} notifications", notifications.size());
        return notifications;
    }

    @GetMapping("/customer/{customerId}")
    public List<Notification> getNotificationsByCustomerId(@PathVariable String customerId) {
        log.info("GET /api/notifications/customer/{} - fetching notifications for customer", customerId);
        List<Notification> notifications = notificationRepository.findByCustomerId(customerId);
        log.info("Found {} notifications for customer {}", notifications.size(), customerId);
        return notifications;
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<Notification> getByNotificationId(@PathVariable String notificationId) {
        log.info("GET /api/notifications/{} - fetching notification by ID", notificationId);
        Optional<Notification> notification = notificationService.getNotificationById(notificationId);
        notification.ifPresent(value -> log.info("Found notification with ID {}", notificationId));
        return notification.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Notification with ID {} not found", notificationId);
                    return ResponseEntity.notFound().build();
                });
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String notificationId) {
        log.info("PATCH /api/notifications/{}/read - marking notification as read", notificationId);
        Notification updatedNotification = notificationService.markAsRead(notificationId);
        log.info("Notification with ID {} marked as read", notificationId);
        return ResponseEntity.ok(updatedNotification);
    }

    @PatchMapping("/customer/{customerId}/read-all")
    public ResponseEntity<List<Notification>> markAllAsRead(@PathVariable String customerId) {
        log.info("PATCH /api/notifications/customer/{}/read-all - marking all notifications as read for customer", customerId);
        List<Notification> updatedNotifications = notificationService.markAllAsRead(customerId);
        log.info("Marked {} notifications as read for customer {}", updatedNotifications.size(), customerId);
        return ResponseEntity.ok(updatedNotifications);
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String notificationId) {
        log.info("DELETE /api/notifications/{} - deleting notification", notificationId);
        notificationService.deleteNotification(notificationId);
        log.info("Notification with ID {} deleted successfully", notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationEvent event) {
        log.info("POST /api/notifications/send - processing notification event for eventType: {}", event.getEventType());
        try {
            notificationService.processNotification(event);
            log.info("Notification processed successfully for eventType: {}", event.getEventType());
            return ResponseEntity.ok("Notification processed successfully for eventType: " + event.getEventType());
        } catch (Exception e) {
            log.error("Failed to process notification for eventType: {}: {}", event.getEventType(), e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to process notification: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        log.info("DELETE /api/notifications - deleting all notifications");
        notificationRepository.deleteAll();
        log.info("All notifications deleted successfully");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sendEmail")
    public ResponseEntity<String> sendNotificationEmail(@RequestBody NotificationEvent event) {
        log.info("POST /api/notifications/sendEmail - manual trigger for eventType: {}", event.getEventType());
        try {
            notificationService.processNotification(event);
            log.info("Manual notification processed successfully for eventType: {}", event.getEventType());
            return ResponseEntity.ok("✅ Notification processed successfully.");
        } catch (Exception e) {
            log.error("Failed to process manual notification for eventType: {}: {}", event.getEventType(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to process notification: " + e.getMessage());
        }
    }
}
