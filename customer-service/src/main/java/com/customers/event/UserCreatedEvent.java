package com.customers.event;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
}
