package com.notification.repository;

import com.notification.entity.NotificationTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {
    //Optional<NotificationTemplate> findByEventTypeAndChannelAndIsActiveTrue(String eventType, String channel);
    List<NotificationTemplate> findByEventTypeAndChannelAndIsActiveTrue(String eventType, String channel);

}
