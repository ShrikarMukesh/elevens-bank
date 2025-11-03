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
    public String getStatus() { return "Card Service Running"; }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> createCard(@RequestBody CardRequest request) {
        log.info("Received request to create a new card for customerId: {}", request.getCustomerId());
        return ResponseEntity.ok(cardService.createCard(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<Card> getCard(@PathVariable Long id) {
        log.info("Received request to get card with id: {}", id);
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCustomerOwner(#customerId)")
    public ResponseEntity<List<Card>> getCardsByCustomer(@PathVariable Long customerId) {
        log.info("Received request to get cards for customerId: {}", customerId);
        return ResponseEntity.ok(cardService.getCardsByCustomerId(customerId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Card>> getAllCards() {
        log.info("Received request to get all cards");
        return ResponseEntity.ok(cardService.getAllCards());
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> blockCard(@PathVariable Long id) {
        log.info("Received request to block card with id: {}", id);
        cardService.blockCard(id);
        return ResponseEntity.ok("Card blocked successfully");
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody CardStatusUpdateRequest req) {
        log.info("Received request to update status for card with id: {} to {}", id, req.getStatus());
        cardService.updateStatus(id, req.getStatus().name());
        return ResponseEntity.ok("Card status updated to " + req.getStatus());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> activateCard(@PathVariable Long id, @RequestBody CardActivateRequest request) {
        log.info("Received request to activate card with id: {}", id);
        cardService.activateCard(id, request.getPin());
        return ResponseEntity.ok("Card activated successfully");
    }

    @PostMapping("/{id}/reset-pin")
    @PreAuthorize("@securityUtils.isOwnerOfCard(#id)")
    public ResponseEntity<String> resetPin(@PathVariable Long id, @RequestBody CardPinResetRequest request) {
        log.info("Received request to reset PIN for card with id: {}", id);
        cardService.resetPin(id, request.getOldPin(), request.getNewPin());
        return ResponseEntity.ok("PIN reset successfully");
    }
//
//    @PostMapping("/{id}/reissue")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Card> reissueCard(@PathVariable Long id) {
//        log.info("Received request to reissue card with id: {}", id);
//        // It's better to move the reissue logic into the CardService
//        // to keep the controller thin.
//        // For example: Card newCard = cardService.reissueCard(id);
//        //Card newCard = cardService.reissueCard(id);
//        return ResponseEntity.ok(newCard);
//    }


}
