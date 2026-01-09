package com.customers.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CustomerEvent(
        String eventType,
        String customerId,
        String userId,
        boolean verified,
        Instant verifiedAt) {
}
