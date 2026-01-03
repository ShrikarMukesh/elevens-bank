package com.accounts.controller;

import com.accounts.dto.AccountRequest;
import com.accounts.dto.AccountTransactionRequest;
import com.accounts.entity.Account;
import com.accounts.entity.AccountType;
import com.accounts.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    // @Mock: Creates a mock object of the class/interface.
    // It doesn't execute real code. We define behavior using
    // when(...).thenReturn(...).
    @Mock
    private AccountServiceImpl accountService;

    // @InjectMocks: Injects the mocks (accountService) into this object
    // (accountController).
    @InjectMocks
    private AccountController accountController;

    // @Spy: Creates a wrapper around a real object.
    // Real methods are called unless stubbed.
    // Here we spy on an ArrayList to demonstrate the concept.
    @Spy
    private List<Account> accountListSpy = new ArrayList<>();

    private Account account;
    private AccountRequest accountRequest;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .accountId(1L)
                .customerId("CUST123")
                .accountNumber("1234567890")
                .accountType(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(1000))
                .currency("INR")
                .build();

        accountRequest = new AccountRequest(
                "CUST123",
                "1234567890",
                AccountType.SAVINGS,
                BigDecimal.valueOf(1000),
                "INR");
    }

    @Test
    void testGetStatus() {
        String status = accountController.getStatus();
        assertEquals("Account Service Running", status);
    }

    @Test
    void testCreateAccount() {
        // Stubbing: Defining behavior for the mock
        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(account);

        ResponseEntity<Account> response = accountController.createAccount(accountRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("1234567890", response.getBody().getAccountNumber());

        // Verification: Ensuring the mock method was called
        verify(accountService, times(1)).createAccount(any(AccountRequest.class));
    }

    @Test
    void testGetAccountsByCustomer() {
        // Using the Spy list
        accountListSpy.add(account);

        // Stubbing the service to return our spied list
        when(accountService.getAccountsByCustomerId("CUST123")).thenReturn(accountListSpy);

        ResponseEntity<List<Account>> response = accountController.getAccountsByCustomer("CUST123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());

        // Verify interaction with the Spy
        verify(accountListSpy).add(account); // Verify add was called on the list
        verify(accountService).getAccountsByCustomerId("CUST123");
    }

    @Test
    void testGetAccount() {
        when(accountService.getAccountById(1L)).thenReturn(account);

        ResponseEntity<Account> response = accountController.getAccount(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getAccountId());
        verify(accountService).getAccountById(1L);
    }

    @Test
    void testGetAllAccounts() {
        List<Account> accounts = List.of(account);
        when(accountService.getAllAccounts()).thenReturn(accounts);

        ResponseEntity<List<Account>> response = accountController.getAllAccounts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(accountService, times(1)).getAllAccounts();
    }

    @Test
    void testDeposit() {
        doNothing().when(accountService).deposit(1L, BigDecimal.valueOf(500));

        ResponseEntity<String> response = accountController.deposit(1L, BigDecimal.valueOf(500));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Deposit successful", response.getBody());
        verify(accountService).deposit(1L, BigDecimal.valueOf(500));
    }

    @Test
    void testWithdraw() {
        doNothing().when(accountService).withdraw(1L, BigDecimal.valueOf(200));

        ResponseEntity<String> response = accountController.withdraw(1L, BigDecimal.valueOf(200));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Withdrawal successful", response.getBody());
        verify(accountService).withdraw(1L, BigDecimal.valueOf(200));
    }

    @Test
    void testTransfer_Success() {
        AccountTransactionRequest request = new AccountTransactionRequest(1L, 2L, BigDecimal.valueOf(300));

        doNothing().when(accountService).transfer(1L, 2L, BigDecimal.valueOf(300));

        ResponseEntity<Map<String, Object>> response = accountController.transfer(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().get("status"));
        verify(accountService).transfer(1L, 2L, BigDecimal.valueOf(300));
    }

    @Test
    void testTransfer_Failure() {
        AccountTransactionRequest request = new AccountTransactionRequest(1L, 2L, BigDecimal.valueOf(300));

        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transfer(1L, 2L, BigDecimal.valueOf(300));

        ResponseEntity<Map<String, Object>> response = accountController.transfer(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("FAILED", response.getBody().get("status"));
        assertEquals("Insufficient funds", response.getBody().get("message"));
    }
}
