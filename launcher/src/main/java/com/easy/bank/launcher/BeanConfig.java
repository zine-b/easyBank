package com.easy.bank.launcher;

import com.easy.bank.domain.port.CustomerRepositoryPort;
import com.easy.bank.domain.usecase.CreateCustomerUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    CreateCustomerUseCase createCustomerUseCase(CustomerRepositoryPort customerRepositoryPort) {
        return new CreateCustomerUseCase(customerRepositoryPort);
    }
}
