package com.easy.bank.domain.exception;

import com.easy.bank.domain.model.CustomerId;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(CustomerId customerId) {
        super("Customer not found: " + customerId.value());
    }
}
