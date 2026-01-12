package com.auth.dto;



public record TokenResponse(String accessToken, String refreshToken, String tokenType, Long expiresIn) {
    public TokenResponse(String accessToken, String refreshToken, Long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
