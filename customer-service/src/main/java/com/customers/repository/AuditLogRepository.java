package com.customers.repository;

import com.customers.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByEventType(String eventType);

    List<AuditLog> findByAffectedEntityTypeAndAffectedEntityId(String entityType, String entityId);

    List<AuditLog> findByUserId(String userId);
}
