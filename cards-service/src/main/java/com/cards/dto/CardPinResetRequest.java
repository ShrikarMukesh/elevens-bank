package com.cards.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public record CardPinResetRequest(String oldPin, String newPin) {
}
