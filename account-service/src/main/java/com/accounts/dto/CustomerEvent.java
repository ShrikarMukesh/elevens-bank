package com.accounts.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class CustomerEvent {
    private String eventType;    // CUSTOMER_CREATED, CUSTOMER_VERIFIED
    private String customerId;
    private String userId;
    private boolean verified;
    private Instant verifiedAt;
}
