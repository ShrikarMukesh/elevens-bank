package com.cards.dto;

import com.cards.entity.CardStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class CardStatusUpdateRequest {
    private CardStatus status;
}
