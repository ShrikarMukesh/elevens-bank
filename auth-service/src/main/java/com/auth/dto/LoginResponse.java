
package com.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiryTime;
}
