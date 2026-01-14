package com.cards.service;

import com.cards.model.Card;
import com.cards.model.CardRequest;

import java.util.List;

public interface CardService {
    Card createCard(CardRequest request);
    Card getCardById(Long id);
    List<Card> getCardsByCustomerId(String customerId);
    List<Card> getCardsByAccountId(Long accountId);
    List<Card> getAllCards();
    void blockCard(Long id);
    void updateStatus(Long id, String status);

    void activateCard(Long id, String pin);
    void resetPin(Long id, String oldPin, String newPin);

    Card reissueCard(Long id);
}
