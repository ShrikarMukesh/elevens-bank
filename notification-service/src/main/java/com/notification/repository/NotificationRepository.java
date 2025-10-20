package com.notification.repository;

import com.notification.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByCustomerId(String customerId);
    Optional<Notification> findByNotificationId(String notificationId);
    boolean existsByNotificationId(String notificationId);
    void deleteByNotificationId(String notificationId);
}
