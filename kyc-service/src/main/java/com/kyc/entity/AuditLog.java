package com.kyc.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    private String userId;      // Who performed the action
    private String resourceId;  // What was accessed (KYC ID / Customer ID)
    private String operation;   // ENCRYPT, DECRYPT, VIEW
    private String field;       // AADHAAR, PAN, etc.
    private String status;      // SUCCESS, FAILURE, DENIED
    private String reason;      // Exception message or access reason
    private Instant timestamp;
}
