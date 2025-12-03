package com.notification.repository;

import com.notification.entity.NotificationTemplate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface NotificationTemplateRepository extends ReactiveMongoRepository<NotificationTemplate, String> {
    Flux<NotificationTemplate> findByEventTypeAndChannelAndIsActiveTrue(String eventType, String channel);
}
