package com.auth.dto;

import lombok.*;

public record AuthRequest(String email, String password) {
}
