package com.easy.bank.domain.usecase;

import com.easy.bank.domain.exception.CustomerNotFoundException;
import com.easy.bank.domain.model.Customer;
import com.easy.bank.domain.model.CustomerId;
import com.easy.bank.domain.port.CustomerRepositoryPort;

public class GetCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public GetCustomerUseCase(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    public Customer getCustomer(CustomerId customerId) {
        return customerRepositoryPort.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
