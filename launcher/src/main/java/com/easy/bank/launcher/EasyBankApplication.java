package com.easy.bank.launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.easy.bank")
public class EasyBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyBankApplication.class, args);
    }
}
