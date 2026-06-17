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
        CustomerRepositoryPort customerRepositoryPort = getCustomerRepositoryPort();
        CreateCustomerUseCase createCustomerUseCase = new CreateCustomerUseCase(customerRepositoryPort);

        CreateCustomerCommand command = new CreateCustomerCommand(
                "firstname",
                "",
                "",
                "",
                "65afe100509a66f663f37114114d6d98a39623e9d14c66eb5a29ffd317fd858c"
        );
        Customer customer = createCustomerUseCase.createCustomer(command);

        Assertions.assertEquals("firstname", customer.firstName());
        Assertions.assertEquals("65afe100509a66f663f37114114d6d98a39623e9d14c66eb5a29ffd317fd858c", customer.passwordHash());
    }

    private static CustomerRepositoryPort getCustomerRepositoryPort() {
        return new CustomerRepositoryPort() {
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
    }
}