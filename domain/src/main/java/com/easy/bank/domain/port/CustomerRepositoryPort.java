package com.easy.bank.domain.port;

import com.easy.bank.domain.model.Customer;
import com.easy.bank.domain.model.CustomerId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepositoryPort {
    List<Customer> findAll();

    Optional<Customer> findById(CustomerId id);

    boolean existsByEmail(String email);

    Customer save(Customer customer);
}
