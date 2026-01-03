package com.cards.dto;

import com.cards.entity.CardStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public record CardStatusUpdateRequest(CardStatus status) {
}
