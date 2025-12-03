package com.accounts.controller;

import com.accounts.entity.Account;
import com.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AccountGraphQLController {

    private final AccountRepository accountRepository;

    @QueryMapping
    public Optional<Account> accountById(@Argument Long accountId) {
        log.info("GraphQL: Fetching account by ID: {}", accountId);
        return accountRepository.findById(accountId);
    }

    @QueryMapping
    public List<Account> accountsByCustomerId(@Argument String customerId) {
        log.info("GraphQL: Fetching accounts for customerId: {}", customerId);
        return accountRepository.findByCustomerId(customerId);
    }

    @QueryMapping
    public List<Account> allAccounts() {
        log.info("GraphQL: Fetching all accounts");
        return accountRepository.findAll();
    }
}
