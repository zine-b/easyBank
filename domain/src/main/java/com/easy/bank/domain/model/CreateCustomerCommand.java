package com.easy.bank.domain.model;

public record CreateCustomerCommand(
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
