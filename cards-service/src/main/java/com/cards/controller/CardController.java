package com.cards.controller;

import com.cards.dto.CardActivateRequest;
import com.cards.dto.CardPinResetRequest;
import com.cards.dto.CardRequest;
import com.cards.dto.CardStatusUpdateRequest;
import com.cards.entity.Card;
import com.cards.entity.CardStatus;
import com.cards.service.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@Slf4j
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/status")
    public String getStatus() {
        log.info("GET /cards/status");
        return "Card Service Running";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> createCard(@RequestBody CardRequest request) {
        log.info("POST /cards/create with request: {}", request);
        Card card = cardService.createCard(request);
        log.info("Card created with id: {}", card.getCardId());
        return ResponseEntity.ok(card);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<Card> getCard(@PathVariable Long id) {
        log.info("GET /cards/{}", id);
        Card card = cardService.getCardById(id);
        log.info("Card found with id: {}", card.getCardId());
        return ResponseEntity.ok(card);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCustomerOwner(#customerId)")
    public ResponseEntity<List<Card>> getCardsByCustomer(@PathVariable Long customerId) {
        log.info("GET /cards/customer/{}", customerId);
        List<Card> cards = cardService.getCardsByCustomerId(customerId);
        log.info("Found {} cards for customerId: {}", cards.size(), customerId);
        return ResponseEntity.ok(cards);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Card>> getAllCards() {
        log.info("GET /cards");
        List<Card> cards = cardService.getAllCards();
        log.info("Found {} cards", cards.size());
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> blockCard(@PathVariable Long id) {
        log.info("POST /cards/{}/block", id);
        cardService.blockCard(id);
        log.info("Card with id: {} blocked successfully", id);
        return ResponseEntity.ok("Card blocked successfully");
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id, @RequestBody CardStatusUpdateRequest req) {
        log.info("POST /cards/{}/status with status: {}", id, req.getStatus());
        cardService.updateStatus(id, req.getStatus().name());
        log.info("Card status updated to {} for cardId: {}", req.getStatus(), id);
        return ResponseEntity.ok("Card status updated to " + req.getStatus());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> activateCard(
            @PathVariable Long id, @RequestBody CardActivateRequest request) {
        log.info("POST /cards/{}/activate", id);
        cardService.activateCard(id, request.getPin());
        log.info("Card with id: {} activated successfully", id);
        return ResponseEntity.ok("Card activated successfully");
    }

    @PostMapping("/{id}/reset-pin")
    @PreAuthorize("@securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> resetPin(
            @PathVariable Long id, @RequestBody CardPinResetRequest request) {
        log.info("POST /cards/{}/reset-pin", id);
        cardService.resetPin(id, request.getOldPin(), request.getNewPin());
        log.info("PIN for cardId: {} reset successfully", id);
        return ResponseEntity.ok("PIN reset successfully");
    }

    @PostMapping("/{id}/reissue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> reissueCard(@PathVariable Long id) {
        log.info("POST /cards/{}/reissue", id);
        Card oldCard = cardService.getCardById(id);
        oldCard.setStatus(CardStatus.BLOCKED);

        Card newCard = new Card();
        newCard.setAccountId(oldCard.getAccountId());
        newCard.setCustomerId(oldCard.getCustomerId());
        newCard.setCardNumber(String.valueOf(System.currentTimeMillis()).substring(4, 20));
        newCard.setCardType(oldCard.getCardType());
        newCard.setNetwork(oldCard.getNetwork());
        newCard.setExpiryDate(oldCard.getExpiryDate().plusYears(3));
        newCard.setPinHash(oldCard.getPinHash());
        newCard.setStatus(CardStatus.ACTIVE);

        Card reissuedCard = cardService.createCard(
                new com.cards.dto.CardRequest() {{
                    setAccountId(oldCard.getAccountId());
                    setCustomerId(oldCard.getCustomerId());
                    setCardType(oldCard.getCardType());
                    setNetwork(oldCard.getNetwork());
                    setExpiryDate(oldCard.getExpiryDate().plusYears(3));
                    setCvv("999");
                    setPinHash(oldCard.getPinHash());
                }}
        );
        log.info("Card with id: {} reissued. New cardId: {}", id, reissuedCard.getCardId());
        return ResponseEntity.ok(reissuedCard);
    }


}
