package com.notification.service;

import com.notification.dto.NotificationEvent;
import com.notification.entity.Notification;
import com.notification.entity.NotificationTemplate;
import com.notification.repository.NotificationRepository;
import com.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final TemplateEngineService templateEngineService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    public List<Notification> getNotificationsByCustomerId(String customerId) {
        log.info("Fetching notifications for customerId: {}", customerId);
        List<Notification> notifications = notificationRepository.findByCustomerId(customerId);
        log.info("Found {} notifications for customerId: {}", notifications.size(), customerId);
        return notifications;
    }

    public Optional<Notification> getNotificationById(String notificationId) {
        log.info("Fetching notification by ID: {}", notificationId);
        Optional<Notification> notification = notificationRepository.findByNotificationId(notificationId);
        notification.ifPresent(n -> log.info("Found notification with ID: {}", notificationId));
        return notification;
    }

    public Notification saveNotification(Notification notification) {
        log.info("Saving notification with ID: {}", notification.getNotificationId());
        notification.setSentAt(Instant.now());
        notification.setStatus("SENT");
        notification.setRetryCount(0);

        if (notification.getAudit() == null) {
            notification.setAudit(Map.of("createdBy", "SYSTEM", "createdAt", Instant.now().toString()));
        }

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification with ID {} saved successfully", savedNotification.getNotificationId());
        return savedNotification;
    }

    @Transactional
    public Notification markAsRead(String notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        notification.setStatus("READ");

        if (notification.getAudit() != null) {
            notification.getAudit().put("updatedAt", Instant.now().toString());
        } else {
            notification.setAudit(Map.of("updatedAt", Instant.now().toString()));
        }

        Notification updatedNotification = notificationRepository.save(notification);
        log.info("Notification with ID {} marked as read", updatedNotification.getNotificationId());
        return updatedNotification;
    }

    @Transactional
    public List<Notification> markAllAsRead(String customerId) {
        log.info("Marking all notifications as read for customerId: {}", customerId);
        List<Notification> notifications = notificationRepository.findByCustomerId(customerId);

        for (Notification n : notifications) {
            if (!"READ".equalsIgnoreCase(n.getStatus())) {
                n.setStatus("READ");
                if (n.getAudit() != null) {
                    n.getAudit().put("updatedAt", Instant.now().toString());
                } else {
                    n.setAudit(Map.of("updatedAt", Instant.now().toString()));
                }
            }
        }

        List<Notification> updatedNotifications = notificationRepository.saveAll(notifications);
        log.info("Marked {} notifications as read for customerId: {}", updatedNotifications.size(), customerId);
        return updatedNotifications;
    }

    @Transactional
    public void deleteNotification(String notificationId) {
        log.info("Deleting notification with ID: {}", notificationId);
        if (!notificationRepository.existsByNotificationId(notificationId)) {
            log.warn("Notification with ID {} not found for deletion", notificationId);
            throw new RuntimeException("Notification not found: " + notificationId);
        }
        notificationRepository.deleteByNotificationId(notificationId);
        log.info("Notification with ID {} deleted successfully", notificationId);
    }

    public void deleteAllByCustomerId(String customerId) {
        log.info("Deleting all notifications for customerId: {}", customerId);
        List<Notification> notifications = notificationRepository.findByCustomerId(customerId);
        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
            log.info("Deleted {} notifications for customerId: {}", notifications.size(), customerId);
        } else {
            log.info("No notifications found for customerId: {} to delete", customerId);
        }
    }

    public void processNotification(NotificationEvent event) {
        log.info("Processing notification event for eventType: {}", event.getEventType());
        List<NotificationTemplate> optTemplate = templateRepository.findByEventTypeAndChannelAndIsActiveTrue(event.getEventType(), event.getChannel());

        NotificationTemplate template;
        if (!optTemplate.isEmpty()) {
            template = optTemplate.get(0);
        } else {
            log.warn("⚠️ No template found for eventType: {}. Using default template.", event.getEventType());
            template = NotificationTemplate.builder()
                    .subject("Notification from Your Bank")
                    .message("Dear Customer, an event of type " + event.getEventType() + " occurred.")
                    .build();
        }

        String subject = templateEngineService.renderTemplate(template.getSubject(), event.getData());
        String message = templateEngineService.renderTemplate(template.getMessage(), event.getData());

        Notification notification = Notification.builder()
                .notificationId("NTF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .eventSource(event.getEventSource())
                .eventType(event.getEventType())
                .customerId(event.getCustomerId())
                .accountId(event.getAccountId())
                .type(event.getChannel())
                .channel(event.getChannel())
                .priority("HIGH")
                .subject(subject)
                .message(message)
                .status("SENT")
                .sentAt(Instant.now())
                .retryCount(0)
                .metadata(new HashMap<>())
                .audit(Map.of("createdAt", Instant.now(), "createdBy", "SYSTEM"))
                .build();

        notificationRepository.save(notification);
        log.info("Notification record created with ID: {}", notification.getNotificationId());

        if ("EMAIL".equalsIgnoreCase(event.getChannel())) {
            log.info("Sending EMAIL to customerId: {}", event.getCustomerId());
            smsService.sendSms(event.getCustomerId(), message);
        } else if ("SMS".equalsIgnoreCase(event.getChannel())) {
            log.info("Sending SMS to customerId: {}", event.getCustomerId());
            smsService.sendSms(event.getCustomerId(), message);
        } else {
            log.warn("⚠️ Unknown channel: {} — skipping send.", event.getChannel());
        }
        log.info("✅ Notification processed and stored successfully for eventType: {}", event.getEventType());
    }
}
