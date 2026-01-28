package com.kyc.service;

import com.kyc.entity.AuditLog;
import com.kyc.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;

    public void logEvent(String userId, String resourceId, String operation, String field, String status, String reason) {
        // Run asynchronously to not block main flow
        CompletableFuture.runAsync(() -> {
            try {
                AuditLog audit = AuditLog.builder()
                        .userId(userId)
                        .resourceId(resourceId)
                        .operation(operation)
                        .field(field)
                        .status(status)
                        .reason(reason)
                        .timestamp(Instant.now())
                        .build();
                
                auditRepository.save(audit);
                log.info("AUDIT: {}", audit);
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        });
    }
}
