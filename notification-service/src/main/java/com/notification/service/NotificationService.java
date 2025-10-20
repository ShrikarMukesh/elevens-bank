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

    // 🔹 Get all notifications for a specific customer
    public List<Notification> getNotificationsByCustomerId(String customerId) {
        return notificationRepository.findByCustomerId(customerId);
    }

    // 🔹 Get one notification by ID
    public Optional<Notification> getNotificationById(String notificationId) {
        return notificationRepository.findByNotificationId(notificationId);
    }

    // 🔹 Create (or save) a new notification
    public Notification saveNotification(Notification notification) {
        notification.setSentAt(Instant.now());
        notification.setStatus("SENT");
        notification.setRetryCount(0);

        // Add minimal audit info if not provided
        if (notification.getAudit() == null) {
            notification.setAudit(Map.of(
                    "createdBy", "SYSTEM",
                    "createdAt", Instant.now().toString()
            ));
        }

        return notificationRepository.save(notification);
    }

    // 🔹 Mark a single notification as READ
    @Transactional
    public Notification markAsRead(String notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        notification.setStatus("READ");

        // Update audit timestamp
        if (notification.getAudit() != null) {
            notification.getAudit().put("updatedAt", Instant.now().toString());
        } else {
            notification.setAudit(Map.of("updatedAt", Instant.now().toString()));
        }

        return notificationRepository.save(notification);
    }

    // 🔹 Mark all notifications for a customer as READ
    @Transactional
    public List<Notification> markAllAsRead(String customerId) {
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

        return notificationRepository.saveAll(notifications);
    }

    // 🔹 Delete a single notification by ID
    @Transactional
    public void deleteNotification(String notificationId) {
        if (!notificationRepository.existsByNotificationId(notificationId)) {
            throw new RuntimeException("Notification not found: " + notificationId);
        }
        notificationRepository.deleteByNotificationId(notificationId);
    }

    // 🔹 Delete all notifications for a specific customer (optional)
    public void deleteAllByCustomerId(String customerId) {
        List<Notification> notifications = notificationRepository.findByCustomerId(customerId);
        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
        }
    }

    public void processNotification(NotificationEvent event) {
        List<NotificationTemplate> optTemplate =
                templateRepository.findByEventTypeAndChannelAndIsActiveTrue(event.getEventType(), event.getChannel());

        NotificationTemplate template;
        if (optTemplate.isEmpty()) {
            template = optTemplate.get(0);
        } else {
            log.warn("⚠️ No template found for eventType: {}. Using default template.", event.getEventType());

            // Use a default template
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

        // Send notification based on channel
        if ("EMAIL".equalsIgnoreCase(event.getChannel())) {
            //emailService.sendEmail(event.getCustomerId(), subject, message);
            smsService.sendSms(event.getCustomerId(), message);
        } else if ("EMAIL".equalsIgnoreCase(event.getChannel())) {
            smsService.sendSms(event.getCustomerId(), message);
        } else {
            log.warn("⚠️ Unknown channel: {} — skipping send.", event.getChannel());
        }
        log.info("✅ Notification stored successfully for eventType: {}", event.getEventType());
    }


}
