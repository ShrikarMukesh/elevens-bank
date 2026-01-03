package com.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationEvent(
        String eventSource,
        String eventType,
        String customerId,
        String accountId,
        String channel,
        Map<String, Object> data,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant eventTime) {
}
