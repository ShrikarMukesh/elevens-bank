package com.cards.dto;


import com.cards.entity.CardType;
import com.cards.entity.Network;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
public class CardRequest {
    private Long accountId;
    private Long customerId;
    private CardType cardType;
    private Network network;
    private LocalDate expiryDate;
    private String cvv;
    private String pinHash;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
}
