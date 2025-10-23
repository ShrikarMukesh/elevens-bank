package com.cards.service.impl;

import com.cards.dto.CardRequest;
import com.cards.entity.Card;
import com.cards.entity.CardStatus;
import com.cards.entity.Network;
import com.cards.repository.CardRepository;
import com.cards.service.CardService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;

    private final PasswordEncoder passwordEncoder;

    public CardServiceImpl(CardRepository cardRepository, PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Card createCard(CardRequest request) {
        Card card = new Card();

        card.setAccountId(request.getAccountId());
        card.setCustomerId(request.getCustomerId());
        card.setCardNumber(generateCardNumber(request.getNetwork()));
        card.setCardType(request.getCardType());
        card.setNetwork(request.getNetwork());
        card.setExpiryDate(request.getExpiryDate());
        card.setCvv(request.getCvv());

        // ✅ Secure PIN Hashing
        card.setPinHash(passwordEncoder.encode(request.getPinHash()));

        card.setDailyLimit(request.getDailyLimit());
        card.setMonthlyLimit(request.getMonthlyLimit());
        card.setStatus(CardStatus.ACTIVE);
        card.setCreatedAt(LocalDate.from(LocalDateTime.now()));

        return cardRepository.save(card);
    }

    /**
     * Generate a Luhn-valid 16-digit card number based on network type.
     */
    private String generateCardNumber(Network network) {
        String binPrefix;

        switch (network) {
            case VISA -> binPrefix = "4";         // VISA cards start with 4
            case MASTERCARD -> binPrefix = "5";   // MasterCard cards start with 5
            case RUPAY -> binPrefix = "6";        // RuPay cards start with 6
            case AMEX -> binPrefix = "3";         // AMEX cards start with 3
            default -> binPrefix = "9";           // fallback (for test cards)
        }

        StringBuilder number = new StringBuilder(binPrefix);
        Random random = new Random();

        // Fill up to 15 digits
        while (number.length() < 15) {
            number.append(random.nextInt(10));
        }

        // ✅ Luhn algorithm for checksum digit
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        number.append(checkDigit);

        return number.toString();
    }


    @Override
    public Card getCardById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
    }

    @Override
    public List<Card> getCardsByCustomerId(Long customerId) {
        return cardRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Card> getCardsByAccountId(Long accountId) {
        return cardRepository.findByAccountId(accountId);
    }

    @Override
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    @Override
    public void blockCard(Long id) {
        Card card = getCardById(id);
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Card card = getCardById(id);
        card.setStatus(CardStatus.valueOf(status.toUpperCase()));
        cardRepository.save(card);
    }


    @Override
    public void activateCard(Long id, String pin) {
        Card card = getCardById(id);

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new RuntimeException("Card is already active");
        }

        card.setPinHash(passwordEncoder.encode(pin));
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    @Override
    public void resetPin(Long id, String oldPin, String newPin) {
        Card card = getCardById(id);

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new RuntimeException("Card must be active to reset PIN");
        }

        if (!passwordEncoder.matches(oldPin, card.getPinHash())) {
            throw new RuntimeException("Invalid old PIN");
        }

        card.setPinHash(passwordEncoder.encode(newPin));
        cardRepository.save(card);
    }
}

