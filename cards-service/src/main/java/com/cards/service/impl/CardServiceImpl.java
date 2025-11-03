package com.cards.service.impl;

import com.cards.dto.CardRequest;
import com.cards.entity.Card;
import com.cards.entity.CardStatus;
import com.cards.entity.Network;
import com.cards.repository.CardRepository;
import com.cards.service.CardService;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Transactional
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;

    private final PasswordEncoder passwordEncoder;

    public CardServiceImpl(CardRepository cardRepository, PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Card createCard(CardRequest request) {
        log.info("Creating a new card for customerId: {}", request.getCustomerId());
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

        Card savedCard = cardRepository.save(card);
        log.info("Successfully created card with id: {}", savedCard.getCardId());
        return savedCard;
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
        log.info("Fetching card with id: {}", id);
        return cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
    }

    @Override
    public List<Card> getCardsByCustomerId(Long customerId) {
        log.info("Fetching cards for customerId: {}", customerId);
        return cardRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Card> getCardsByAccountId(Long accountId) {
        log.info("Fetching cards for accountId: {}", accountId);
        return cardRepository.findByAccountId(accountId);
    }

    @Override
    public List<Card> getAllCards() {
        log.info("Fetching all cards");
        return cardRepository.findAll();
    }

    @Override
    public void blockCard(Long id) {
        log.info("Blocking card with id: {}", id);
        Card card = getCardById(id);
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        log.info("Successfully blocked card with id: {}", id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        log.info("Updating status for card with id: {} to {}", id, status);
        Card card = getCardById(id);
        card.setStatus(CardStatus.valueOf(status.toUpperCase()));
        cardRepository.save(card);
        log.info("Successfully updated status for card with id: {} to {}", id, status);
    }


    @Override
    public void activateCard(Long id, String pin) {
        log.info("Activating card with id: {}", id);
        Card card = getCardById(id);

        if (card.getStatus() == CardStatus.ACTIVE) {
            log.warn("Attempted to activate an already active card with id: {}", id);
            throw new RuntimeException("Card is already active");
        }

        card.setPinHash(passwordEncoder.encode(pin));
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
        log.info("Successfully activated card with id: {}", id);
    }

    @Override
    public void resetPin(Long id, String oldPin, String newPin) {
        log.info("Resetting PIN for card with id: {}", id);
        Card card = getCardById(id);

        if (card.getStatus() != CardStatus.ACTIVE) {
            log.warn("Attempted to reset PIN for an inactive card with id: {}", id);
            throw new RuntimeException("Card must be active to reset PIN");
        }

        if (!passwordEncoder.matches(oldPin, card.getPinHash())) {
            log.warn("Invalid old PIN provided for card with id: {}", id);
            throw new RuntimeException("Invalid old PIN");
        }

        card.setPinHash(passwordEncoder.encode(newPin));
        cardRepository.save(card);
        log.info("Successfully reset PIN for card with id: {}", id);
    }

    @Override
    public Card reissueCard(Long id) {
        log.info("Reissuing card for original card id: {}", id);
        Card oldCard = getCardById(id);

        // Block the old card
        blockCard(oldCard.getCardId());

        // Create a new card request from the old card's details
        CardRequest newCardRequest = new CardRequest();
        newCardRequest.setCustomerId(oldCard.getCustomerId());
        newCardRequest.setAccountId(oldCard.getAccountId());
        newCardRequest.setCardType(oldCard.getCardType());
        newCardRequest.setNetwork(oldCard.getNetwork());
        newCardRequest.setExpiryDate(LocalDate.now().plusYears(3));
        newCardRequest.setCvv(String.valueOf(new Random().nextInt(900) + 100));
        newCardRequest.setPinHash(oldCard.getPinHash()); // Note: Re-using pin hash, user doesn't need to set a new one.
        newCardRequest.setDailyLimit(oldCard.getDailyLimit());
        newCardRequest.setMonthlyLimit(oldCard.getMonthlyLimit());

        log.info("Creating new card to replace card id: {}", id);
        return createCard(newCardRequest);
    }
}
