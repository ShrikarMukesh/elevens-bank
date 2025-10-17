package com.auth.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    private String userId;      // 🔄 changed from Long → String
    private String username;
    private String fullName;    // 🔄 added
    private String email;
    private String phone;
    private LocalDateTime createdAt;
}
