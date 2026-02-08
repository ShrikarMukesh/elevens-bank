package com.customers.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    @Field("service_name")
    private String serviceName;

    @Field("event_type")
    private String eventType;

    @Field("user_id")
    private String userId;

    @Field("affected_entity_type")
    private String affectedEntityType;

    @Field("affected_entity_id")
    private String affectedEntityId;

    @Field("old_value")
    private String oldValue;

    @Field("new_value")
    private String newValue;

    @Field("status_code")
    private Integer statusCode;

    @Field("request_payload")
    private String requestPayload;

    @Field("response_payload")
    private String responsePayload;

    @Field("error_message")
    private String errorMessage;

    @Field("ip_address")
    private String ipAddress;

    @Field("description")
    private String description;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;
}
