package com.notification.repository;

import com.notification.entity.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
    Flux<Notification> findByCustomerId(String customerId);

    Mono<Notification> findByNotificationId(String notificationId);

    Mono<Boolean> existsByNotificationId(String notificationId);

    Mono<Void> deleteByNotificationId(String notificationId);
}
