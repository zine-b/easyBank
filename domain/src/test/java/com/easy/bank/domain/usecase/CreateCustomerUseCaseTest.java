package com.easy.bank.domain.usecase;

import com.easy.bank.domain.model.CreateCustomerCommand;
import com.easy.bank.domain.model.Customer;
import com.easy.bank.domain.model.CustomerId;
import com.easy.bank.domain.port.CustomerRepositoryPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;


class CreateCustomerUseCaseTest {

    @Test
    void createCustomer() {
        CustomerRepositoryPort port = new CustomerRepositoryPort() {
            @Override
            public Optional<Customer> findById(CustomerId id) {
                return Optional.empty();
            }

            @Override
            public boolean existsByEmail(String email) {
                return false;
            }

            @Override
            public Customer save(Customer customer) {
                return customer;
            }
        };
        CreateCustomerUseCase createCustomerUseCase = new CreateCustomerUseCase(port);

        CreateCustomerCommand command = new CreateCustomerCommand(
                "firstname",
                "",
                "",
                ""
        );
        Customer customer = createCustomerUseCase.createCustomer(command);

        Assertions.assertEquals("firstname", customer.firstName());
    }
}