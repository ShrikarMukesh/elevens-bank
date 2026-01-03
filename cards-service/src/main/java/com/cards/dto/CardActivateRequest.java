package com.cards.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public record CardActivateRequest(String pin) {
}
