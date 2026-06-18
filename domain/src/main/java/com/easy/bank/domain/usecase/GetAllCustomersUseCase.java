package com.easy.bank.domain.usecase;

import com.easy.bank.domain.model.Customer;
import com.easy.bank.domain.port.CustomerRepositoryPort;

import java.util.List;

public class GetAllCustomersUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public GetAllCustomersUseCase(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    public List<Customer> getAllCustomers() {
        return customerRepositoryPort.findAll();
    }
}
