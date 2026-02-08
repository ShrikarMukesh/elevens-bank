package com.transaction.repository;

import com.transaction.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEventType(String eventType);

    List<AuditLog> findByAffectedEntityTypeAndAffectedEntityId(String entityType, String entityId);

    List<AuditLog> findByUserId(String userId);
}
