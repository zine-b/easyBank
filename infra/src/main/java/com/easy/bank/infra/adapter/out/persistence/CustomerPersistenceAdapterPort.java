package com.easy.bank.infra.adapter.out.persistence;

import com.easy.bank.domain.model.CustomerId;
import com.easy.bank.domain.port.CustomerRepositoryPort;
import com.easy.bank.domain.model.Customer;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class CustomerPersistenceAdapterPort implements CustomerRepositoryPort {

    private final SpringDataCustomerRepository springDataRepository;

    CustomerPersistenceAdapterPort(SpringDataCustomerRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return springDataRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    @Override
    public Customer save(Customer customer) {
        return toDomain(springDataRepository.save(toEntity(customer)));
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        return new Customer(
                new CustomerId(entity.getId()),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCreatedAt()
        );
    }

    private CustomerJpaEntity toEntity(Customer customer) {
        return new CustomerJpaEntity(
                customer.id().value(),
                customer.firstName(),
                customer.lastName(),
                customer.email(),
                customer.phone(),
                customer.createdAt()
        );
    }
}
