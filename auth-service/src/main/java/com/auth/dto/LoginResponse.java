
package com.auth.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LoginResponse(String accessToken, String refreshToken, LocalDateTime expiryTime) {
}
