package com.easy.bank.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI easyBankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("easyBank API")
                        .description("API REST de la banque en ligne easyBank — gestion des clients, comptes, virements et cartes.")
                        .version("v1")
                        .contact(new Contact()
                                .name("easyBank")
                                .email("api@easybank.fr")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local")));
    }
}
