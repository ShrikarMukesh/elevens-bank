package com.cards.controller;

import com.cards.dto.CardActivateRequest;
import com.cards.dto.CardPinResetRequest;
import com.cards.dto.CardRequest;
import com.cards.dto.CardStatusUpdateRequest;
import com.cards.entity.Card;
import com.cards.entity.CardStatus;
import com.cards.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/status")
    public String getStatus() { return "Card Service Running"; }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> createCard(@RequestBody CardRequest request) {
        return ResponseEntity.ok(cardService.createCard(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<Card> getCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCustomerOwner(#customerId)")
    public ResponseEntity<List<Card>> getCardsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(cardService.getCardsByCustomerId(customerId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Card>> getAllCards() {
        return ResponseEntity.ok(cardService.getAllCards());
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> blockCard(@PathVariable Long id) {
        cardService.blockCard(id);
        return ResponseEntity.ok("Card blocked successfully");
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id, @RequestBody CardStatusUpdateRequest req) {
        cardService.updateStatus(id, req.getStatus().name());
        return ResponseEntity.ok("Card status updated to " + req.getStatus());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> activateCard(
            @PathVariable Long id, @RequestBody CardActivateRequest request) {
        cardService.activateCard(id, request.getPin());
        return ResponseEntity.ok("Card activated successfully");
    }

    @PostMapping("/{id}/reset-pin")
    @PreAuthorize("@securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> resetPin(
            @PathVariable Long id, @RequestBody CardPinResetRequest request) {
        cardService.resetPin(id, request.getOldPin(), request.getNewPin());
        return ResponseEntity.ok("PIN reset successfully");
    }

    @PostMapping("/{id}/reissue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> reissueCard(@PathVariable Long id) {
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

        return ResponseEntity.ok(cardService.createCard(
                new com.cards.dto.CardRequest() {{
                    setAccountId(oldCard.getAccountId());
                    setCustomerId(oldCard.getCustomerId());
                    setCardType(oldCard.getCardType());
                    setNetwork(oldCard.getNetwork());
                    setExpiryDate(oldCard.getExpiryDate().plusYears(3));
                    setCvv("999");
                    setPinHash(oldCard.getPinHash());
                }}
        ));
    }


}

