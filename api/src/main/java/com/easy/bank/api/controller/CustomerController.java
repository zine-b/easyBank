package com.easy.bank.api.controller;

import com.easy.bank.api.dto.CreateCustomerRequest;
import com.easy.bank.api.dto.CustomerResponse;
import com.easy.bank.domain.model.CreateCustomerCommand;
import com.easy.bank.domain.model.Customer;
import com.easy.bank.domain.usecase.CreateCustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Création et gestion des clients easyBank")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;

    public CustomerController(CreateCustomerUseCase createCustomerUseCase) {
        this.createCustomerUseCase = createCustomerUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Créer un client",
            description = "Inscrit un nouveau client. L'e-mail doit être unique — 409 EMAIL_ALREADY_USED si doublon."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Client créé avec succès",
                    headers = @Header(name = "Location", description = "/api/v1/customers/{customerId}"),
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation échouée (champ vide, e-mail invalide, téléphone invalide)", content = @Content),
            @ApiResponse(responseCode = "409", description = "E-mail déjà utilisé", content = @Content)
    })
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody @Valid CreateCustomerRequest request) {
        Customer customer = createCustomerUseCase.createCustomer(new CreateCustomerCommand(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone()
        ));
        return ResponseEntity
                .status(201).body(CustomerResponse.from(customer));
    }
}
