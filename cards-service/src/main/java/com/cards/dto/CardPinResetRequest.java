package com.cards.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class CardPinResetRequest {
    private String oldPin;
    private String newPin;
}
