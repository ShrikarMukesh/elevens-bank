package com.cards.dto;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class CardActivateRequest {
    private String pin;
}

