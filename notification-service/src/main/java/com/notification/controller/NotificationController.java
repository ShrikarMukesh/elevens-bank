package com.notification.controller;

import com.notification.dto.NotificationEvent;
import com.notification.entity.Notification;
import com.notification.service.NotificationService;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @GetMapping
    public Flux<Notification> getAll() {
        log.info("GET /api/notifications - fetching all notifications");
        return notificationRepository.findAll()
                .doOnComplete(() -> log.info("Finished fetching all notifications"));
    }

    @GetMapping("/customer/{customerId}")
    public Flux<Notification> getNotificationsByCustomerId(@PathVariable String customerId) {
        log.info("GET /api/notifications/customer/{} - fetching notifications for customer", customerId);
        return notificationService.getNotificationsByCustomerId(customerId)
                .doOnComplete(() -> log.info("Finished fetching notifications for customer {}", customerId));
    }

    @GetMapping("/{notificationId}")
    public Mono<ResponseEntity<Notification>> getByNotificationId(@PathVariable String notificationId) {
        log.info("GET /api/notifications/{} - fetching notification by ID", notificationId);
        return notificationService.getNotificationById(notificationId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Found notification with ID {}", notificationId);
                    } else {
                        log.warn("Notification with ID {} not found", notificationId);
                    }
                });
    }

    @PatchMapping("/{notificationId}/read")
    public Mono<ResponseEntity<Notification>> markAsRead(@PathVariable String notificationId) {
        log.info("PATCH /api/notifications/{}/read - marking notification as read", notificationId);
        return notificationService.markAsRead(notificationId)
                .map(ResponseEntity::ok)
                .doOnSuccess(n -> log.info("Notification with ID {} marked as read", notificationId));
    }

    @PatchMapping("/customer/{customerId}/read-all")
    public Mono<ResponseEntity<Flux<Notification>>> markAllAsRead(@PathVariable String customerId) {
        log.info("PATCH /api/notifications/customer/{}/read-all - marking all notifications as read for customer",
                customerId);
        Flux<Notification> updatedNotifications = notificationService.markAllAsRead(customerId);
        return Mono.just(ResponseEntity.ok(updatedNotifications))
                .doOnSuccess(v -> log.info("Marking notifications as read for customer {}", customerId));
    }

    @DeleteMapping("/{notificationId}")
    public Mono<ResponseEntity<Void>> deleteNotification(@PathVariable String notificationId) {
        log.info("DELETE /api/notifications/{} - deleting notification", notificationId);
        return notificationService.deleteNotification(notificationId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnSuccess(v -> log.info("Notification with ID {} deleted successfully", notificationId));
    }

    @PostMapping("/send")
    public Mono<ResponseEntity<String>> sendNotification(@RequestBody NotificationEvent event) {
        log.info("POST /api/notifications/send - processing notification event for eventType: {}",
                event.getEventType());
        return notificationService.processNotification(event)
                .then(Mono.just(ResponseEntity
                        .ok("Notification processed successfully for eventType: " + event.getEventType())))
                .doOnSuccess(
                        v -> log.info("Notification processed successfully for eventType: {}", event.getEventType()))
                .onErrorResume(e -> {
                    log.error("Failed to process notification for eventType: {}: {}", event.getEventType(),
                            e.getMessage(), e);
                    return Mono
                            .just(ResponseEntity.status(500).body("Failed to process notification: " + e.getMessage()));
                });
    }

    @DeleteMapping
    public Mono<ResponseEntity<Void>> deleteAll() {
        log.info("DELETE /api/notifications - deleting all notifications");
        return notificationRepository.deleteAll()
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnSuccess(v -> log.info("All notifications deleted successfully"));
    }

    @PostMapping("/sendEmail")
    public Mono<ResponseEntity<String>> sendNotificationEmail(@RequestBody NotificationEvent event) {
        log.info("POST /api/notifications/sendEmail - manual trigger for eventType: {}", event.getEventType());
        return notificationService.processNotification(event)
                .then(Mono.just(ResponseEntity.ok("✅ Notification processed successfully.")))
                .doOnSuccess(v -> log.info("Manual notification processed successfully for eventType: {}",
                        event.getEventType()))
                .onErrorResume(e -> {
                    log.error("Failed to process manual notification for eventType: {}: {}", event.getEventType(),
                            e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("❌ Failed to process notification: " + e.getMessage()));
                });
    }
}
