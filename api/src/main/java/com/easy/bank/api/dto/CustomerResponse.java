package com.easy.bank.api.dto;

import com.easy.bank.domain.model.Customer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Données du client créé")
public record CustomerResponse(
        @Schema(description = "Identifiant unique du client (UUID)", example = "2c4a1d90-7e6b-4b6a-9f2d-1a2b3c4d5e6f") UUID customerId,
        @Schema(description = "Prénom", example = "Ahmed") String firstName,
        @Schema(description = "Nom de famille", example = "Benali") String lastName,
        @Schema(description = "Adresse e-mail", example = "ahmed.benali@example.com") String email,
        @Schema(description = "Numéro de téléphone", example = "+33612345678") String phone,
        @Schema(description = "Date et heure de création (UTC)", example = "2026-06-17T09:00:00Z") Instant createdAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id().value(),
                customer.firstName(),
                customer.lastName(),
                customer.email(),
                customer.phone(),
                customer.createdAt()
        );
    }
}
