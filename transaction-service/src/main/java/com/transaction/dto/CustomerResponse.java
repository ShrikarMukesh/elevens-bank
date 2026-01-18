package com.transaction.dto;

public record CustomerResponse(
    String customerId,
    String firstName,
    String lastName,
    String email,
    String status
) {}
