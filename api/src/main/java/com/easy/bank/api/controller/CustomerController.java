package com.easy.bank.api.controller;

import com.easy.bank.api.dto.CreateCustomerRequest;
import com.easy.bank.api.dto.CustomerResponse;
import com.easy.bank.domain.model.CreateCustomerCommand;
import com.easy.bank.domain.model.Customer;
import com.easy.bank.domain.model.CustomerId;
import com.easy.bank.domain.usecase.CreateCustomerUseCase;
import com.easy.bank.domain.usecase.GetAllCustomersUseCase;
import com.easy.bank.domain.usecase.GetCustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Création et gestion des clients easyBank")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final GetAllCustomersUseCase getAllCustomersUseCase;

    public CustomerController(CreateCustomerUseCase createCustomerUseCase,
                               GetCustomerUseCase getCustomerUseCase,
                               GetAllCustomersUseCase getAllCustomersUseCase) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.getCustomerUseCase = getCustomerUseCase;
        this.getAllCustomersUseCase = getAllCustomersUseCase;
    }

    @PostMapping
    @Operation(summary = "Créer un client",
            description = "Inscrit un nouveau client. L'e-mail doit être unique — 409 EMAIL_ALREADY_USED si doublon.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client créé avec succès",
                    headers = @Header(name = "Location", description = "/api/v1/customers/{customerId}"),
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation échouée", content = @Content),
            @ApiResponse(responseCode = "409", description = "E-mail déjà utilisé", content = @Content)
    })
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody @Valid CreateCustomerRequest request) {
        Customer customer = createCustomerUseCase.createCustomer(new CreateCustomerCommand(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.passwordHash()
        ));
        return ResponseEntity.status(201).body(CustomerResponse.from(customer));
    }

    @GetMapping
    @Operation(summary = "Lister tous les clients",
            description = "Retourne la liste complète des clients. Retourne une liste vide si aucun client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des clients",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CustomerResponse.class))))
    })
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> customers = getAllCustomersUseCase.getAllCustomers()
                .stream()
                .map(CustomerResponse::from)
                .toList();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Récupérer un client par ID",
            description = "Retourne les informations d'un client à partir de son UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client trouvé",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Client introuvable", content = @Content)
    })
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID customerId) {
        Customer customer = getCustomerUseCase.getCustomer(new CustomerId(customerId));
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
