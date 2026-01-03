package com.accounts.service.impl;

import com.accounts.dto.AccountRequest;
import com.accounts.entity.Account;
import com.accounts.entity.AccountStatus;
import com.accounts.entity.AccountType;
import com.accounts.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

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
                .status(AccountStatus.ACTIVE)
                .build();

        accountRequest = new AccountRequest(
                "CUST123",
                "1234567890",
                AccountType.SAVINGS,
                BigDecimal.valueOf(1000),
                "INR");
    }

    @Test
    void testCreateAccount() {
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        Account createdAccount = accountService.createAccount(accountRequest);

        assertNotNull(createdAccount);
        assertEquals("CUST123", createdAccount.getCustomerId());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testGetAccountsByCustomerId() {
        when(accountRepository.findByCustomerId("CUST123")).thenReturn(List.of(account));

        List<Account> accounts = accountService.getAccountsByCustomerId("CUST123");

        assertFalse(accounts.isEmpty());
        assertEquals(1, accounts.size());
        verify(accountRepository).findByCustomerId("CUST123");
    }

    @Test
    void testGetAccountById_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Account foundAccount = accountService.getAccountById(1L);

        assertNotNull(foundAccount);
        assertEquals(1L, foundAccount.getAccountId());
    }

    @Test
    void testGetAccountById_NotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.getAccountById(1L));
    }

    @Test
    void testDeposit_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.deposit(1L, BigDecimal.valueOf(500));

        assertEquals(BigDecimal.valueOf(1500), account.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    void testWithdraw_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.withdraw(1L, BigDecimal.valueOf(500));

        assertEquals(BigDecimal.valueOf(500), account.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    void testWithdraw_InsufficientFunds() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(RuntimeException.class, () -> accountService.withdraw(1L, BigDecimal.valueOf(2000)));

        // Verify save was NEVER called
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void testTransfer_Success() {
        Account toAccount = Account.builder()
                .accountId(2L)
                .customerId("CUST456")
                .accountNumber("0987654321")
                .balance(BigDecimal.valueOf(500))
                .build();

        // Mocking findByIdForUpdate for locking
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        accountService.transfer(1L, 2L, BigDecimal.valueOf(200));

        assertEquals(BigDecimal.valueOf(800), account.getBalance());
        assertEquals(BigDecimal.valueOf(700), toAccount.getBalance());

        verify(accountRepository, times(2)).saveAndFlush(any(Account.class));
    }
}
