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
