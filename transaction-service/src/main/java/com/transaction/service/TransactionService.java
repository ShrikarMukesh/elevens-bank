package com.transaction.service;

import com.transaction.clients.AccountClient;
import com.transaction.dto.AccountTransactionRequest;
import com.transaction.dto.TransactionRequest;
import com.transaction.entity.Transaction;
import com.transaction.entity.TransactionStatus;
import com.transaction.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    @Transactional
    public Transaction performTransaction(TransactionRequest request) {

        Transaction txn = Transaction.builder()
                .accountId(request.getAccountId())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .modeId(request.getModeId())
                .referenceNumber(UUID.randomUUID().toString())
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        txn = transactionRepository.save(txn);

        try {
            switch (request.getTransactionType()) {
                case DEPOSIT -> accountClient.deposit(request.getAccountId(), request.getAmount());
                case WITHDRAWAL -> accountClient.withdraw(request.getAccountId(), request.getAmount());
                case TRANSFER -> accountClient.transfer(
                        new AccountTransactionRequest(
                                request.getAccountId(),
                                request.getTargetAccountId(),
                                request.getAmount()
                        ));
            }

            txn.setStatus(TransactionStatus.SUCCESS);

        } catch (Exception e) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setDescription("Account service failed: " + e.getMessage());
        }

        return transactionRepository.save(txn);
    }
}

