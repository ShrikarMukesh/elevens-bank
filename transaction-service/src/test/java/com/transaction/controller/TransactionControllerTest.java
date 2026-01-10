package com.transaction.controller;

import com.transaction.dto.TransactionRequest;
import com.transaction.entity.Transaction;
import com.transaction.entity.TransactionType;
import com.transaction.service.TransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionControllerTest {

    @Mock
    TransactionService transactionService;

    @InjectMocks
    TransactionController transactionController;

    public Transaction transactionFixture(){
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionId(12345L);
        return transaction1;
    }

    public List<Transaction> listOfTransactionsFixture(){

        List<Transaction> transactionList = new ArrayList<>();

        Transaction transaction1 = new Transaction();
        transaction1.setTransactionId(12345L);
        transactionList.add(transaction1);
        return transactionList;
    }

    @Test
    public void performTransaction_PositiveTestCase(){
        TransactionRequest request = new TransactionRequest(
                101L,
                null,
                BigDecimal.valueOf(100.00),
                TransactionType.DEPOSIT,
                1,
                "Test Deposit"
        );

        Transaction expectedTransaction = transactionFixture();
        when(transactionService.performTransaction(request)).thenReturn(expectedTransaction);

        ResponseEntity<Transaction> response = transactionController.performTransaction(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(expectedTransaction, response.getBody());
    }

    @Test
    public void getTransactionsByAccountId_PositiveTestCase(){
         when(transactionService.getTransactionsByAccountId(101L)).thenReturn(listOfTransactionsFixture());

         ResponseEntity<List<Transaction>> response = transactionController.getTransactionsByAccountId(101L);

         Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
         Assertions.assertNotNull(response.getBody());
         Assertions.assertEquals(1, response.getBody().size());
    }

}
