package com.transaction.controller;

import com.transaction.dto.TransactionRequest;
import com.transaction.entity.Transaction;
import com.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> performTransaction(@RequestBody @Valid TransactionRequest request) {
        Transaction txn = transactionService.performTransaction(request);
        return ResponseEntity.ok(txn);
    }
}

