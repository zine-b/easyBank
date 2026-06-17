package com.easy.bank.domain.usecase;

import com.easy.bank.domain.exception.EmailAlreadyUsedException;
import com.easy.bank.domain.model.Customer;
import com.easy.bank.domain.model.CustomerId;
import com.easy.bank.domain.model.CreateCustomerCommand;
import com.easy.bank.domain.port.CustomerRepositoryPort;

import java.time.Instant;
import java.util.UUID;


public class CreateCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public CreateCustomerUseCase(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    public Customer createCustomer(CreateCustomerCommand command) {
        if (customerRepositoryPort.existsByEmail(command.email())) {
            throw new EmailAlreadyUsedException(command.email());
        }
        Customer customer = new Customer(
                new CustomerId(UUID.randomUUID()),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phone(),
                command.passwordHash(),
                Instant.now()
        );

        return customerRepositoryPort.save(customer);
    }
}
