package com.easy.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Corps de la requête pour créer un nouveau client")
public record CreateCustomerRequest(
        @Schema(description = "Prénom", example = "Ahmed") @NotBlank String firstName,
        @Schema(description = "Nom de famille", example = "Benali") @NotBlank String lastName,
        @Schema(description = "Adresse e-mail unique", example = "ahmed.benali@example.com") @Email @NotBlank String email,
        @Schema(description = "Numéro de téléphone (10 à 15 chiffres, + en option)", example = "+33612345678")
        @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Invalid phone format") String phone,
        @Schema(example = "65afe100509a66f663f37114114d6d98a39623e9d14c66eb5a29ffd317fd858c")
        String passwordHash
) {
}
