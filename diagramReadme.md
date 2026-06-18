# easyBank — Diagrammes d'architecture

---

## 1. Présentation de l'application

```mermaid
graph TB
    Client(["🖥️  Client HTTP\n(Postman / Frontend)"])

    subgraph APP["easyBank — Banque 100 % en ligne"]
        direction TB

        subgraph ENDPOINTS["API REST  /api/v1"]
            E1["POST   /customers\n➜ Créer un client"]
            E2["GET    /customers\n➜ Lister tous les clients"]
            E3["GET    /customers/{id}\n➜ Récupérer un client"]
        end

        subgraph DOMAIN_RULES["Règles métier"]
            R1["✔  Un e-mail est unique"]
            R2["✔  Un client introuvable → 404"]
            R3["✔  Validation des champs à l'entrée"]
        end

        subgraph TECH["Stack technique"]
            T1["Spring Boot 4 · Java 25"]
            T2["Spring Data JPA · H2 / PostgreSQL"]
            T3["Swagger UI  /swagger-ui.html"]
            T4["JUnit 5 · Tests d'intégration"]
        end
    end

    Client -->|"JSON  application/json"| E1
    Client -->|"GET"| E2
    Client -->|"GET"| E3
    E1 & E2 & E3 --> DOMAIN_RULES
    DOMAIN_RULES --> T2
```

---

## 2. Architecture hexagonale — modules et classes

```mermaid
graph TB

    HTTP(["🖥️  Client HTTP"])

    subgraph LAUNCHER["📦 launcher  —  point d'entrée Spring Boot"]
        App["EasyBankApplication\n@SpringBootApplication\n@EntityScan  @EnableJpaRepositories"]
        Beans["BeanConfig  @Configuration\ncrée les @Bean des use cases"]
    end

    subgraph API["📦 api  —  adaptateurs entrants HTTP"]
        Controller["CustomerController  @RestController\n─────────────────────────────\nPOST   /api/v1/customers\nGET    /api/v1/customers\nGET    /api/v1/customers/{id}"]
        GEH["GlobalExceptionHandler\n@RestControllerAdvice\n─────────────────────\n409  EMAIL_ALREADY_USED\n404  CUSTOMER_NOT_FOUND\n400  VALIDATION_ERROR"]
        Swagger["OpenApiConfig\n→ /swagger-ui.html"]
    end

    subgraph DOMAIN["📦 domain  —  cœur métier  ·  Pure Java  ·  zéro framework"]

        subgraph UC["Use Cases"]
            UC1["CreateCustomerUseCase"]
            UC2["GetCustomerUseCase"]
            UC3["GetAllCustomersUseCase"]
        end

        subgraph PORT["Port sortant  ≪interface≫"]
            Repo["CustomerRepositoryPort\n──────────────────────\nfindAll() : List\nfindById() : Optional\nexistsByEmail() : boolean\nsave() : Customer"]
        end

        subgraph MODEL["Modèles & Value Objects"]
            Customer["Customer  (record)\nid · firstName · lastName\nemail · phone · passwordHash\ncreatedAt"]
            CustomerId["CustomerId  (record)\nvalue : UUID"]
            Command["CreateCustomerCommand  (record)"]
        end

        subgraph EX["Exceptions métier"]
            Ex1["EmailAlreadyUsedException"]
            Ex2["CustomerNotFoundException"]
        end
    end

    subgraph INFRA["📦 infra  —  adaptateurs sortants JPA"]
        Adapter["CustomerPersistenceAdapterPort\n@Component  implements CustomerRepositoryPort\n──────────────────────────────────────────\nfindAll()  findById()  save()  existsByEmail()\ntoDomain()  toEntity()"]
        SDRepo["SpringDataCustomerRepository\nextends JpaRepository"]
        Entity["CustomerJpaEntity  @Entity\n@Table customers"]
    end

    DB[("💾  H2 / PostgreSQL")]

    %% ── Flux HTTP ──────────────────────────────
    HTTP -->|"HTTP Request"| Controller
    Controller -->|"appelle"| UC1
    Controller -->|"appelle"| UC2
    Controller -->|"appelle"| UC3

    %% ── Use Cases → Port ───────────────────────
    UC1 -->|"utilise"| Repo
    UC2 -->|"utilise"| Repo
    UC3 -->|"utilise"| Repo

    %% ── Port ← Adapter ─────────────────────────
    Adapter -.->|"implémente"| Repo

    %% ── Infra → DB ─────────────────────────────
    Adapter --> SDRepo --> Entity --> DB

    %% ── BeanConfig crée les use cases ──────────
    Beans -->|"new"| UC1
    Beans -->|"new"| UC2
    Beans -->|"new"| UC3

    %% ── Règle de dépendance ────────────────────
    API -.->|"dépend de"| DOMAIN
    INFRA -.->|"dépend de"| DOMAIN
    LAUNCHER -.->|"dépend de"| API
    LAUNCHER -.->|"dépend de"| INFRA
```

> **Règle de dépendance** : les flèches pointent toujours **vers l'intérieur**.
> `INFRA` et `API` connaissent `DOMAIN` — jamais l'inverse.
> `api` et `infra` ne se connaissent **pas** entre eux.

---

## 3. Flux détaillé — Créer un client

```mermaid
sequenceDiagram
    actor Client
    participant C  as CustomerController
    participant UC as CreateCustomerUseCase
    participant P  as CustomerRepositoryPort
    participant A  as CustomerPersistenceAdapterPort
    participant DB as H2 / PostgreSQL

    Client->>C: POST /api/v1/customers<br/>{firstName, lastName, email, phone, passwordHash}

    C->>C: @Valid → vérifie email, @NotBlank...
    Note over C: 400 si validation échoue

    C->>UC: createCustomer(CreateCustomerCommand)

    UC->>P: existsByEmail(email)
    P->>A: existsByEmail(email)
    A->>DB: SELECT count(*) FROM customers WHERE email = ?
    DB-->>A: 0
    A-->>P: false
    P-->>UC: false

    UC->>UC: new Customer(CustomerId.generate(), ..., Instant.now())

    UC->>P: save(customer)
    P->>A: save(customer)
    A->>A: toEntity(customer) → CustomerJpaEntity
    A->>DB: INSERT INTO customers
    DB-->>A: entity sauvegardée
    A->>A: toDomain(entity) → Customer
    A-->>P: Customer
    P-->>UC: Customer
    UC-->>C: Customer

    C-->>Client: 201 Created<br/>CustomerResponse {customerId, firstName, ...}

    Note over Client,DB: Si email déjà utilisé :<br/>UC lance EmailAlreadyUsedException<br/>→ GlobalExceptionHandler → 409 EMAIL_ALREADY_USED
```

---

## 4. Flux détaillé — Récupérer un client par ID

```mermaid
sequenceDiagram
    actor Client
    participant C  as CustomerController
    participant UC as GetCustomerUseCase
    participant P  as CustomerRepositoryPort
    participant A  as CustomerPersistenceAdapterPort
    participant DB as H2 / PostgreSQL

    Client->>C: GET /api/v1/customers/{customerId}

    C->>UC: getCustomer(new CustomerId(uuid))
    UC->>P: findById(customerId)
    P->>A: findById(customerId)
    A->>DB: SELECT * FROM customers WHERE id = ?
    DB-->>A: row

    alt Client trouvé
        A->>A: toDomain(entity) → Customer
        A-->>P: Optional.of(customer)
        P-->>UC: Optional.of(customer)
        UC-->>C: Customer
        C-->>Client: 200 OK  CustomerResponse
    else Client introuvable
        DB-->>A: empty
        A-->>P: Optional.empty()
        P-->>UC: Optional.empty()
        UC->>UC: throw CustomerNotFoundException
        UC-->>C: CustomerNotFoundException
        C-->>Client: 404 CUSTOMER_NOT_FOUND
    end
```

---

## 5. Modèle de domaine

```mermaid
classDiagram
    class Customer {
        <<record>>
        +CustomerId id
        +String firstName
        +String lastName
        +String email
        +String phone
        +String passwordHash
        +Instant createdAt
    }

    class CustomerId {
        <<record>>
        +UUID value
    }

    class CreateCustomerCommand {
        <<record>>
        +String firstName
        +String lastName
        +String email
        +String phone
        +String passwordHash
    }

    class CustomerRepositoryPort {
        <<interface>>
        +findAll() List~Customer~
        +findById(CustomerId) Optional~Customer~
        +existsByEmail(String) boolean
        +save(Customer) Customer
    }

    class CreateCustomerUseCase {
        -CustomerRepositoryPort port
        +createCustomer(CreateCustomerCommand) Customer
    }

    class GetCustomerUseCase {
        -CustomerRepositoryPort port
        +getCustomer(CustomerId) Customer
    }

    class GetAllCustomersUseCase {
        -CustomerRepositoryPort port
        +getAllCustomers() List~Customer~
    }

    class EmailAlreadyUsedException {
        <<RuntimeException>>
    }

    class CustomerNotFoundException {
        <<RuntimeException>>
        +CustomerNotFoundException(CustomerId)
    }

    Customer *-- CustomerId : contient
    CreateCustomerUseCase --> CustomerRepositoryPort : utilise
    GetCustomerUseCase --> CustomerRepositoryPort : utilise
    GetAllCustomersUseCase --> CustomerRepositoryPort : utilise
    CreateCustomerUseCase ..> CreateCustomerCommand : consomme
    CreateCustomerUseCase ..> EmailAlreadyUsedException : lance
    GetCustomerUseCase ..> CustomerNotFoundException : lance
```
