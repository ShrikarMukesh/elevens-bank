package com.cards.service;

import com.cards.dto.CardRequest;
import com.cards.entity.Card;

import java.util.List;

public interface CardService {
    Card createCard(CardRequest request);
    Card getCardById(Long id);
    List<Card> getCardsByCustomerId(Long customerId);
    List<Card> getCardsByAccountId(Long accountId);
    List<Card> getAllCards();
    void blockCard(Long id);
    void updateStatus(Long id, String status);

    void activateCard(Long id, String pin);
    void resetPin(Long id, String oldPin, String newPin);

    Card reissueCard(Long id);
}
