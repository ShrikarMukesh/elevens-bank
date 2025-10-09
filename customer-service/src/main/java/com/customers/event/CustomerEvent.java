package com.customers.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEvent {
    private String eventType;   // e.g. CUSTOMER_CREATED, CUSTOMER_VERIFIED
    private String customerId;
    private String userId;
    private boolean verified;
    private Instant verifiedAt;
}
