package com.customers.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Builder
public record CustomerEvent(
        String eventType,
        String customerId,
        String userId,
        boolean verified,
        Instant verifiedAt) {
}
