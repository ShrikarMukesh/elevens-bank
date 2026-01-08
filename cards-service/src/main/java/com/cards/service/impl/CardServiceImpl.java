package com.cards.service.impl;

import com.cards.model.Card;
import com.cards.model.CardRequest;
import com.cards.model.CardStatus;
import com.cards.model.CardType;
import com.cards.model.Network;
import com.cards.repository.CardRepository;
import com.cards.service.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
        com.cards.entity.Card card = new com.cards.entity.Card();

        card.setAccountId(request.getAccountId());
        card.setCustomerId(request.getCustomerId());

        // Map model enums to entity enums
        com.cards.entity.CardType entityCardType = com.cards.entity.CardType.valueOf(request.getCardType().name());
        com.cards.entity.Network entityNetwork = com.cards.entity.Network.valueOf(request.getNetwork().name());

        card.setCardNumber(generateCardNumber(entityNetwork));
        card.setCardType(entityCardType);
        card.setNetwork(entityNetwork);
        card.setExpiryDate(request.getExpiryDate());
        card.setCvv(request.getCvv());

        // ✅ Secure PIN Hashing
        card.setPinHash(passwordEncoder.encode(request.getPinHash()));

        card.setDailyLimit(request.getDailyLimit());
        card.setMonthlyLimit(request.getMonthlyLimit());
        card.setStatus(com.cards.entity.CardStatus.ACTIVE);
        card.setCreatedAt(LocalDate.from(LocalDateTime.now()));

        com.cards.entity.Card savedCard = cardRepository.save(card);
        log.info("Successfully created card with id: {}", savedCard.getCardId());
        return mapToModel(savedCard);
    }

    /**
     * Generate a Luhn-valid 16-digit card number based on network type.
     */
    private String generateCardNumber(com.cards.entity.Network network) {
        String binPrefix;

        switch (network) {
            case VISA -> binPrefix = "4"; // VISA cards start with 4
            case MASTERCARD -> binPrefix = "5"; // MasterCard cards start with 5
            case RUPAY -> binPrefix = "6"; // RuPay cards start with 6
            case AMEX -> binPrefix = "3"; // AMEX cards start with 3
            default -> binPrefix = "9"; // fallback (for test cards)
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
                if (n > 9)
                    n = (n % 10) + 1;
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
        com.cards.entity.Card entity = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        return mapToModel(entity);
    }

    @Override
    public List<Card> getCardsByCustomerId(Long customerId) {
        log.info("Fetching cards for customerId: {}", customerId);
        return cardRepository.findByCustomerId(customerId).stream()
                .map(this::mapToModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Card> getCardsByAccountId(Long accountId) {
        log.info("Fetching cards for accountId: {}", accountId);
        return cardRepository.findByAccountId(accountId).stream()
                .map(this::mapToModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Card> getAllCards() {
        log.info("Fetching all cards");
        return cardRepository.findAll().stream()
                .map(this::mapToModel)
                .collect(Collectors.toList());
    }

    @Override
    public void blockCard(Long id) {
        log.info("Blocking card with id: {}", id);
        com.cards.entity.Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setStatus(com.cards.entity.CardStatus.BLOCKED);
        cardRepository.save(card);
        log.info("Successfully blocked card with id: {}", id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        log.info("Updating status for card with id: {} to {}", id, status);
        com.cards.entity.Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setStatus(com.cards.entity.CardStatus.valueOf(status.toUpperCase()));
        cardRepository.save(card);
        log.info("Successfully updated status for card with id: {} to {}", id, status);
    }

    @Override
    public void activateCard(Long id, String pin) {
        log.info("Activating card with id: {}", id);
        com.cards.entity.Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (card.getStatus() == com.cards.entity.CardStatus.ACTIVE) {
            log.warn("Attempted to activate an already active card with id: {}", id);
            throw new RuntimeException("Card is already active");
        }

        card.setPinHash(passwordEncoder.encode(pin));
        card.setStatus(com.cards.entity.CardStatus.ACTIVE);
        cardRepository.save(card);
        log.info("Successfully activated card with id: {}", id);
    }

    @Override
    public void resetPin(Long id, String oldPin, String newPin) {
        log.info("Resetting PIN for card with id: {}", id);
        com.cards.entity.Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (card.getStatus() != com.cards.entity.CardStatus.ACTIVE) {
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
        com.cards.entity.Card oldCard = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // Block the old card
        oldCard.setStatus(com.cards.entity.CardStatus.BLOCKED);
        cardRepository.save(oldCard);

        // Create a new card request from the old card's details
        CardRequest newCardRequest = new CardRequest();
        newCardRequest.setAccountId(oldCard.getAccountId());
        newCardRequest.setCustomerId(oldCard.getCustomerId());

        // Map entity enums back to model enums
        newCardRequest.setCardType(CardType.valueOf(oldCard.getCardType().name()));
        newCardRequest.setNetwork(Network.valueOf(oldCard.getNetwork().name()));

        newCardRequest.setExpiryDate(LocalDate.now().plusYears(3));
        newCardRequest.setCvv(String.valueOf(new Random().nextInt(900) + 100));
        newCardRequest.setPinHash(oldCard.getPinHash());
        newCardRequest.setDailyLimit(oldCard.getDailyLimit());
        newCardRequest.setMonthlyLimit(oldCard.getMonthlyLimit());

        log.info("Creating new card to replace card id: {}", id);
        return createCard(newCardRequest);
    }

    private Card mapToModel(com.cards.entity.Card entity) {
        Card model = new Card();
        model.setCardId(entity.getCardId());
        model.setAccountId(entity.getAccountId());
        model.setCustomerId(entity.getCustomerId());
        model.setCardNumber(entity.getCardNumber());
        model.setCardType(CardType.valueOf(entity.getCardType().name()));
        model.setNetwork(Network.valueOf(entity.getNetwork().name()));
        model.setExpiryDate(entity.getExpiryDate());
        model.setCvv(entity.getCvv());
        model.setPinHash(entity.getPinHash());
        model.setDailyLimit(entity.getDailyLimit());
        model.setMonthlyLimit(entity.getMonthlyLimit());
        model.setStatus(CardStatus.valueOf(entity.getStatus().name()));
        model.setCreatedAt(entity.getCreatedAt());
        return model;
    }
}
