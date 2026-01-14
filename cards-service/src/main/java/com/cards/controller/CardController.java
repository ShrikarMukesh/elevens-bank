package com.cards.controller;

import com.cards.api.CardsApi;
import com.cards.model.CardActivateRequest;
import com.cards.model.CardPinResetRequest;
import com.cards.model.CardRequest;
import com.cards.model.CardStatusUpdateRequest;
import com.cards.model.Card;
import com.cards.service.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class CardController implements CardsApi {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @Override
    public ResponseEntity<String> getStatus() {
        log.info("GET /cards/status");
        return ResponseEntity.ok("Card Service Running");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> createCard(CardRequest request) {
        log.info("POST /cards/create with request: {}", request);
        Card card = cardService.createCard(request);
        log.info("Card created with id: {}", card.getCardId());
        return ResponseEntity.ok(card);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<Card> getCard(Long id) {
        log.info("GET /cards/{}", id);
        Card card = cardService.getCardById(id);
        log.info("Card found with id: {}", card.getCardId());
        return ResponseEntity.ok(card);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCustomerOwner(#customerId)")
    public ResponseEntity<List<Card>> getCardsByCustomer(String customerId) {
        log.info("GET /cards/customer/{}", customerId);
        List<Card> cards = cardService.getCardsByCustomerId(customerId);
        log.info("Found {} cards for customerId: {}", cards.size(), customerId);
        return ResponseEntity.ok(cards);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Card>> getAllCards() {
        log.info("GET /cards");
        List<Card> cards = cardService.getAllCards();
        log.info("Found {} cards", cards.size());
        return ResponseEntity.ok(cards);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> blockCard(Long id) {
        log.info("POST /cards/{}/block", id);
        cardService.blockCard(id);
        log.info("Card with id: {} blocked successfully", id);
        return ResponseEntity.ok("Card blocked successfully");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateStatus(Long id, CardStatusUpdateRequest req) {
        log.info("POST /cards/{}/status with status: {}", id, req.getStatus());
        cardService.updateStatus(id, req.getStatus().name());
        log.info("Card status updated to {} for cardId: {}", req.getStatus(), id);
        return ResponseEntity.ok("Card status updated to " + req.getStatus());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> activateCard(Long id, CardActivateRequest request) {
        log.info("POST /cards/{}/activate", id);
        cardService.activateCard(id, request.getPin());
        log.info("Card with id: {} activated successfully", id);
        return ResponseEntity.ok("Card activated successfully");
    }

    @Override
    @PreAuthorize("@securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> resetPin(Long id, CardPinResetRequest request) {
        log.info("POST /cards/{}/reset-pin", id);
        cardService.resetPin(id, request.getOldPin(), request.getNewPin());
        log.info("PIN for cardId: {} reset successfully", id);
        return ResponseEntity.ok("PIN reset successfully");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> reissueCard(Long id) {
        log.info("POST /cards/{}/reissue", id);
        Card reissuedCard = cardService.reissueCard(id);
        log.info("Card with id: {} reissued. New cardId: {}", id, reissuedCard.getCardId());
        return ResponseEntity.ok(reissuedCard);
    }
}
