package com.auth.dto;

import lombok.*;

public record RegisterRequest(String email, String password, String role) {
}
