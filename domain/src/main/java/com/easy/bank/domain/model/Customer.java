package com.easy.bank.domain.model;


import java.time.Instant;

public record Customer(
        CustomerId id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String passwordHash,
        Instant createdAt
) {
}
