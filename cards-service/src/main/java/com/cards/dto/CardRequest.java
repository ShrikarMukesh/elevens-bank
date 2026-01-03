package com.cards.dto;

import com.cards.entity.CardType;
import com.cards.entity.Network;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardRequest(
        Long accountId,
        Long customerId,
        CardType cardType,
        Network network,
        LocalDate expiryDate,
        String cvv,
        String pinHash,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit) {
}
