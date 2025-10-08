package com.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEvent {

    private String eventSource;
    private String eventType;
    private String customerId;
    private String accountId;
    private String channel;
    private Map<String, Object> data; // e.g. amount, mode, maskedAccount, etc.
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant eventTime;
}

