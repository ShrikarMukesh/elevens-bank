package com.cards.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Setter
@Getter
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cardId;

    private Long accountId;
    private Long customerId;

    @Column(unique = true, length = 16, nullable = false)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    private Network network;

    private LocalDate expiryDate;
    private String cvv;
    private String pinHash;

    private BigDecimal dailyLimit = BigDecimal.valueOf(50000);
    private BigDecimal monthlyLimit = BigDecimal.valueOf(200000);

    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;

    private LocalDate createdAt = LocalDate.now();

    // Getters & Setters
    // ...
}
