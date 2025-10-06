package com.transaction.controller;

import com.transaction.dto.TransactionRequest;
import com.transaction.entity.Transaction;
import com.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> performTransaction(@RequestBody @Valid TransactionRequest request) {
        log.info("Received transaction request: {}", request);
        Transaction txn = transactionService.performTransaction(request);
        log.info("Transaction processed successfully. ID: {}", txn.getTransactionId());
        return ResponseEntity.ok(txn);
    }
}

