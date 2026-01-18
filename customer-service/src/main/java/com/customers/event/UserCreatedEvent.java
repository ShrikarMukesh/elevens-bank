package com.customers.event;

import lombok.*;

import java.time.LocalDateTime;

@Builder
public record UserCreatedEvent(
        String userId,
        String username,
        String email,
        String fullName,
        String phone,
        LocalDateTime createdAt) {
}
