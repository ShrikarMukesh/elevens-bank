package com.notification.service;

import com.notification.dto.CustomerDto;
import com.notification.dto.NotificationEvent;
import com.notification.entity.Notification;
import com.notification.entity.NotificationTemplate;
import com.notification.repository.NotificationRepository;
import com.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final TemplateEngineService templateEngineService;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    public Flux<Notification> getNotificationsByCustomerId(String customerId) {
        log.info("Fetching notifications for customerId: {}", customerId);
        return notificationRepository.findByCustomerId(customerId)
                .doOnComplete(() -> log.info("Finished fetching notifications for customerId: {}", customerId));
    }

    public Mono<Notification> getNotificationById(String notificationId) {
        log.info("Fetching notification by ID: {}", notificationId);
        return notificationRepository.findByNotificationId(notificationId)
                .doOnNext(n -> log.info("Found notification with ID: {}", notificationId));
    }

    public Mono<Notification> saveNotification(Notification notification) {
        log.info("Saving notification with ID: {}", notification.getNotificationId());
        notification.setSentAt(Instant.now());
        notification.setStatus("SENT");
        notification.setRetryCount(0);

        if (notification.getAudit() == null) {
            notification.setAudit(Map.of("createdBy", "SYSTEM", "createdAt", Instant.now().toString()));
        }

        return notificationRepository.save(notification)
                .doOnNext(saved -> log.info("Notification with ID {} saved successfully", saved.getNotificationId()));
    }

    public Mono<Notification> markAsRead(String notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        return notificationRepository.findByNotificationId(notificationId)
                .switchIfEmpty(Mono.error(new RuntimeException("Notification not found: " + notificationId)))
                .flatMap(notification -> {
                    notification.setStatus("READ");
                    if (notification.getAudit() != null) {
                        notification.getAudit().put("updatedAt", Instant.now().toString());
                    } else {
                        notification.setAudit(Map.of("updatedAt", Instant.now().toString()));
                    }
                    return notificationRepository.save(notification);
                })
                .doOnNext(updated -> log.info("Notification with ID {} marked as read", updated.getNotificationId()));
    }

    public Flux<Notification> markAllAsRead(String customerId) {
        log.info("Marking all notifications as read for customerId: {}", customerId);
        return notificationRepository.findByCustomerId(customerId)
                .filter(n -> !"READ".equalsIgnoreCase(n.getStatus()))
                .flatMap(n -> {
                    n.setStatus("READ");
                    if (n.getAudit() != null) {
                        n.getAudit().put("updatedAt", Instant.now().toString());
                    } else {
                        n.setAudit(Map.of("updatedAt", Instant.now().toString()));
                    }
                    return notificationRepository.save(n);
                })
                .doOnComplete(() -> log.info("Marked notifications as read for customerId: {}", customerId));
    }

    public Mono<Void> deleteNotification(String notificationId) {
        log.info("Deleting notification with ID: {}", notificationId);
        return notificationRepository.existsByNotificationId(notificationId)
                .flatMap(exists -> {
                    if (!exists) {
                        log.warn("Notification with ID {} not found for deletion", notificationId);
                        return Mono.error(new RuntimeException("Notification not found: " + notificationId));
                    }
                    return notificationRepository.deleteByNotificationId(notificationId);
                })
                .doOnSuccess(v -> log.info("Notification with ID {} deleted successfully", notificationId));
    }

    public Mono<Void> deleteAllByCustomerId(String customerId) {
        log.info("Deleting all notifications for customerId: {}", customerId);
        return notificationRepository.findByCustomerId(customerId)
                .collectList()
                .flatMap(notifications -> {
                    if (!notifications.isEmpty()) {
                        return notificationRepository.deleteAll(notifications)
                                .doOnSuccess(v -> log.info("Deleted {} notifications for customerId: {}",
                                        notifications.size(), customerId));
                    } else {
                        log.info("No notifications found for customerId: {} to delete", customerId);
                        return Mono.empty();
                    }
                });
    }

    public Mono<Void> processNotification(NotificationEvent event) {
        log.info("Processing notification event for eventType: {}", event.eventType());
        return templateRepository.findByEventTypeAndChannelAndIsActiveTrue(event.eventType(), event.channel())
                .next() // Get the first one or empty
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("⚠️ No template found for eventType: {}. Using default template.", event.eventType());
                    return Mono.just(NotificationTemplate.builder()
                            .subject("Notification from Your Bank")
                            .message("Dear Customer, an event of type " + event.eventType() + " occurred.")
                            .build());
                }))
                .flatMap(template -> {
                    String subject = templateEngineService.renderTemplate(template.getSubject(), event.data());
                    String message = templateEngineService.renderTemplate(template.getMessage(), event.data());

                    Notification notification = Notification.builder()
                            .notificationId("NTF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .eventSource(event.eventSource())
                            .eventType(event.eventType())
                            .customerId(event.customerId())
                            .accountId(event.accountId())
                            .type(event.channel())
                            .channel(event.channel())
                            .priority("HIGH")
                            .subject(subject)
                            .message(message)
                            .status("SENT")
                            .sentAt(Instant.now())
                            .retryCount(0)
                            .metadata(new HashMap<>())
                            .audit(Map.of("createdAt", Instant.now(), "createdBy", "SYSTEM"))
                            .build();

                    return notificationRepository.save(notification)
                            .doOnNext(saved -> log.info("Notification record created with ID: {}",
                                    saved.getNotificationId()))
                            .flatMap(saved -> fetchCustomerDetails(event.customerId())
                                    .flatMap(customerDto -> {
                                        if ("EMAIL".equalsIgnoreCase(event.channel())) {
                                            log.info("Sending EMAIL to customerId: {} at {}", event.customerId(), customerDto.getEmail());
                                            emailService.sendEmail(customerDto.getEmail(), subject, message);
                                        } else if ("SMS".equalsIgnoreCase(event.channel())) {
                                            if (customerDto.getPhoneNumbers() != null) {
                                                String mobileNumber = customerDto.getPhoneNumbers().stream()
                                                        .filter(CustomerDto.PhoneNumberDto::isPrimary)
                                                        .findFirst()
                                                        .map(CustomerDto.PhoneNumberDto::getNumber)
                                                        .orElse(null);
                                                if (mobileNumber != null) {
                                                    log.info("Sending SMS to customerId: {} at {}", event.customerId(), mobileNumber);
                                                    smsService.sendSms(mobileNumber, message);
                                                } else {
                                                    log.warn("No primary mobile number found for customerId: {}", event.customerId());
                                                }
                                            } else {
                                                log.warn("No phone numbers found for customerId: {}", event.customerId());
                                            }
                                        }
                                        return Mono.empty();
                                    }));
                })
                .doOnSuccess(v -> log.info("✅ Notification processed and stored successfully for eventType: {}",
                        event.eventType()))
                .then();
    }

    private Mono<CustomerDto> fetchCustomerDetails(String customerId) {
        return webClientBuilder.build()
                .get()
                .uri("http://customer-service/api/customers/" + customerId)
                .retrieve()
                .bodyToMono(CustomerDto.class)
                .doOnError(e -> log.error("Error fetching customer details for customerId: {}", customerId, e));
    }
}
