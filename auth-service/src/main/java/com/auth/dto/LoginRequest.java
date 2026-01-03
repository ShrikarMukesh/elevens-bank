package com.auth.dto;

import lombok.Data;

public record LoginRequest(String username, String password) {
}
