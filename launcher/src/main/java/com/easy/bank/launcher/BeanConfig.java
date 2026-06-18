package com.easy.bank.launcher;

import com.easy.bank.domain.port.CustomerRepositoryPort;
import com.easy.bank.domain.usecase.CreateCustomerUseCase;
import com.easy.bank.domain.usecase.GetAllCustomersUseCase;
import com.easy.bank.domain.usecase.GetCustomerUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    CreateCustomerUseCase createCustomerUseCase(CustomerRepositoryPort customerRepositoryPort) {
        return new CreateCustomerUseCase(customerRepositoryPort);
    }

    @Bean
    GetCustomerUseCase getCustomerUseCase(CustomerRepositoryPort customerRepositoryPort) {
        return new GetCustomerUseCase(customerRepositoryPort);
    }

    @Bean
    GetAllCustomersUseCase getAllCustomersUseCase(CustomerRepositoryPort customerRepositoryPort) {
        return new GetAllCustomersUseCase(customerRepositoryPort);
    }
}
