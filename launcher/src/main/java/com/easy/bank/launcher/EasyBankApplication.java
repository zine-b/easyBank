package com.easy.bank.launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = "com.easy.bank.infra.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "com.easy.bank.infra.adapter.out.persistence")
@SpringBootApplication(scanBasePackages = "com.easy.bank")
public class EasyBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyBankApplication.class, args);
    }
}
