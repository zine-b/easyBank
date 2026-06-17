# easyBank — Squelette d'architecture hexagonale (Ports & Adapters)

> Guide de conception pour l'entretien Java — applique l'archi hexagonale (Alistair Cockburn)
> au domaine easyBank. Stack cible : Java 21 / Spring Boot 3.

---

## 1. L'idée en une phrase

Le **cœur métier (domaine)** est au centre et ne dépend de **rien** (ni Spring, ni JPA, ni
web). Le monde extérieur communique avec lui **uniquement via des interfaces appelées
« ports »**. Les technologies concrètes (REST, base de données, PDF…) sont des
**« adapters »** branchés sur ces ports.

> Bénéfice : on peut changer la base, le framework web ou le moteur PDF **sans toucher au
> métier**, et tester le métier **sans démarrer Spring ni base de données**.

---

## 2. La règle de dépendance (le point clé à l'oral)

Les dépendances pointent **toujours vers l'intérieur** :

```
        infrastructure  ──►  application  ──►  domain
        (adapters)          (use cases +        (entités,
                             ports)              règles métier)
```

- `domain` ne dépend de **personne**.
- `application` ne dépend **que** de `domain`.
- `infrastructure` dépend de `application` (elle **implémente** les ports ou les **appelle**).

C'est l'**inversion de dépendances** : le domaine définit *ce dont il a besoin* (un port
`AccountRepository`), et l'infrastructure fournit *le comment* (un adapter JPA).

```
                    ┌───────────── Driving / Primary side ─────────────┐
   Controller REST ─┤                                                  │
   (adapter IN)     │   ┌──────────────────────────────────────────┐   │
                    └──►│  PORT IN (use case)   ◄── appelé par l'IN │   │
                        │        │                                  │   │
                        │        ▼                                  │   │
                        │   ┌─────────────┐      Domaine pur        │   │
                        │   │ APPLICATION │  ◄──►  (Account,         │   │
                        │   │  (service)  │        Money, Iban…)     │   │
                        │   └─────────────┘                         │   │
                        │        │                                  │   │
                        │        ▼                                  │   │
                        │  PORT OUT (repository, pdf) ──► implémenté │   │
                        └──────────────────────────────────────────┘   │
                    ┌────────────────── Driven / Secondary side ────────┘
   Adapter JPA  ────┤  (AccountPersistenceAdapter, ITextPdfGenerator…)
   (adapter OUT)
```

---

## 3. Structure de packages easyBank

```
com.easybank
│
├── domain                              # CŒUR — zéro dépendance framework
│   ├── model
│   │   ├── Account.java                # entité riche (porte les règles)
│   │   ├── Customer.java
│   │   ├── Beneficiary.java
│   │   ├── Card.java
│   │   └── Transfer.java
│   ├── vo                              # Value Objects (immuables)
│   │   ├── AccountId.java   CustomerId.java   BeneficiaryId.java
│   │   ├── Iban.java        Bic.java
│   │   ├── Money.java
│   │   └── Offer.java       CardType.java   AccountStatus.java
│   └── exception
│       ├── InsufficientFundsException.java
│       ├── AccountClosedException.java
│       ├── ActiveAccountExistsException.java
│       └── NonZeroBalanceException.java
│
├── application                         # USE CASES + PORTS
│   ├── port
│   │   ├── in                          # driving ports (1 interface par use case)
│   │   │   ├── OpenAccountUseCase.java
│   │   │   ├── MakeTransferUseCase.java
│   │   │   ├── IssueCardUseCase.java
│   │   │   ├── AddBeneficiaryUseCase.java
│   │   │   ├── ChangeOfferUseCase.java
│   │   │   ├── CloseAccountUseCase.java
│   │   │   └── GenerateStatementUseCase.java
│   │   └── out                         # driven ports (besoins du métier)
│   │       ├── AccountRepository.java
│   │       ├── CustomerRepository.java
│   │       ├── BeneficiaryRepository.java
│   │       ├── CardRepository.java
│   │       ├── TransferRepository.java
│   │       ├── IbanGenerator.java
│   │       └── StatementPdfGenerator.java
│   └── service                         # implémentations des use cases
│       ├── OpenAccountService.java
│       ├── MakeTransferService.java
│       ├── IssueCardService.java
│       └── …
│
└── infrastructure                      # ADAPTERS (le « comment »)
    ├── adapter
    │   ├── in
    │   │   └── web                      # primary adapters
    │   │       ├── AccountController.java
    │   │       ├── TransferController.java
    │   │       ├── dto                  # Request / Response (jamais l'entité !)
    │   │       │   ├── TransferRequest.java
    │   │       │   └── TransferResponse.java
    │   │       ├── mapper               # DTO ↔ Command/Domain (MapStruct)
    │   │       └── GlobalExceptionHandler.java   # exceptions → RFC 7807
    │   └── out
    │       ├── persistence              # secondary adapters (BDD)
    │       │   ├── AccountJpaEntity.java
    │       │   ├── AccountJpaRepository.java     # extends JpaRepository
    │       │   ├── AccountPersistenceAdapter.java # implements AccountRepository
    │       │   └── mapper               # JpaEntity ↔ Domain
    │       └── pdf
    │           └── ITextStatementPdfGenerator.java # implements StatementPdfGenerator
    └── config
        └── BeanConfiguration.java
```

> 💡 Variante de découpage : **par feature** (`com.easybank.account.{domain,application,infrastructure}`,
> `com.easybank.transfer.…`). Même principe, frontières par module métier. À mentionner comme
> évolution vers une archi « modular monolith ».

---

## 4. Les trois couches

### 4.1 `domain` — le métier pur
- Entités **riches** : les règles vivent dans les objets, pas dans des services anémiques.
  `account.debit(money)` lève `InsufficientFundsException` si besoin.
- **Value Objects** immuables : `Iban`, `Bic`, `Money`, `Offer` — auto‑validants.
- **Aucune** annotation `@Entity`, `@Component`, `@Autowired` ici. Java pur.

### 4.2 `application` — l'orchestration
- Définit les **ports IN** (un par use case) et les **ports OUT** (besoins en persistance/PDF…).
- Les **services** orchestrent : chargent via les ports OUT, appellent le domaine, sauvegardent.
- Porte la **transaction** (`@Transactional`) et l'idempotence.

### 4.3 `infrastructure` — la technique
- **Adapters IN** : controllers REST → traduisent HTTP en *commands* et appellent les ports IN.
- **Adapters OUT** : implémentent les ports OUT (JPA, iText…).
- DTO web ≠ entité JPA ≠ modèle domaine : **trois représentations distinctes**, reliées par mappers.

---

## 5. Liste des ports easyBank

**Ports IN (driving)** — un par cas d'usage de la spec :
`OpenAccountUseCase`, `AddBeneficiaryUseCase`, `MakeTransferUseCase`, `IssueCardUseCase`,
`ChangeOfferUseCase`, `CloseAccountUseCase`, `GetRibUseCase`, `GenerateStatementUseCase`.

**Ports OUT (driven)** — ce dont le métier a besoin du monde extérieur :
`CustomerRepository`, `AccountRepository`, `BeneficiaryRepository`, `CardRepository`,
`TransferRepository`, `IbanGenerator`, `StatementPdfGenerator`, `RibPdfGenerator`.

---

## 6. Tranche verticale complète : le virement

Du controller jusqu'à la base, en suivant la règle de dépendance. (Stubs illustratifs.)

### 6.1 Domaine — entité riche

```java
// domain/model/Account.java
public class Account {
    private final AccountId id;
    private final CustomerId customerId;
    private final Iban iban;
    private Offer offer;
    private Money balance;
    private AccountStatus status;
    private long version;            // verrou optimiste

    public void debit(Money amount) {
        if (status != AccountStatus.ACTIVE) throw new AccountClosedException(id);
        if (balance.isLessThan(amount))     throw new InsufficientFundsException(id, balance, amount);
        if (amount.isGreaterThan(offer.transferLimit()))
            throw new LimitExceededException(offer, amount);
        this.balance = balance.minus(amount);
    }

    public void close() {
        if (!balance.isZero()) throw new NonZeroBalanceException(id);
        this.status = AccountStatus.CLOSED;
    }
    // getters, pas de setter public sur balance/status
}
```

### 6.2 Port IN + command

```java
// application/port/in/MakeTransferUseCase.java
public interface MakeTransferUseCase {
    TransferResult makeTransfer(MakeTransferCommand command);
}

// application/port/in/MakeTransferCommand.java  (immuable)
public record MakeTransferCommand(
        AccountId sourceAccountId,
        Iban targetIban,
        Money amount,
        String label,
        String idempotencyKey) {}
```

### 6.3 Ports OUT

```java
// application/port/out/AccountRepository.java
public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    Optional<Account> findActiveByCustomerId(CustomerId customerId);
    Account save(Account account);
}

// application/port/out/BeneficiaryRepository.java
public interface BeneficiaryRepository {
    Optional<Beneficiary> findByAccountIdAndIban(AccountId accountId, Iban iban);
}
```

### 6.4 Service applicatif (orchestration + transaction)

```java
// application/service/MakeTransferService.java
@Service
public class MakeTransferService implements MakeTransferUseCase {

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransferRepository transferRepository;

    public MakeTransferService(AccountRepository a, BeneficiaryRepository b, TransferRepository t) {
        this.accountRepository = a; this.beneficiaryRepository = b; this.transferRepository = t;
    }

    @Override
    @Transactional                         // débit + écriture = atomique
    public TransferResult makeTransfer(MakeTransferCommand cmd) {
        Account source = accountRepository.findById(cmd.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(cmd.sourceAccountId()));

        // RG-08 : virement externe ⇒ bénéficiaire enregistré
        beneficiaryRepository.findByAccountIdAndIban(source.getId(), cmd.targetIban())
                .orElseThrow(() -> new BeneficiaryNotFoundException(cmd.targetIban()));

        source.debit(cmd.amount());        // toutes les règles sont DANS le domaine
        accountRepository.save(source);

        Transfer transfer = Transfer.execute(
                source.getId(), cmd.targetIban(), cmd.amount(), cmd.label());
        return TransferResult.from(transferRepository.save(transfer));
    }
}
```

### 6.5 Adapter IN (REST) — dépend du PORT, pas du service concret

```java
// infrastructure/adapter/in/web/TransferController.java
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/transfers")
class TransferController {

    private final MakeTransferUseCase makeTransfer;   // ◄── le PORT IN

    TransferController(MakeTransferUseCase makeTransfer) { this.makeTransfer = makeTransfer; }

    @PostMapping
    ResponseEntity<TransferResponse> transfer(
            @PathVariable UUID accountId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid TransferRequest req) {

        TransferResult result = makeTransfer.makeTransfer(req.toCommand(accountId, idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(result));
    }
}
```

### 6.6 Adapter OUT (persistence) — implémente le port

```java
// infrastructure/adapter/out/persistence/AccountPersistenceAdapter.java
@Component
class AccountPersistenceAdapter implements AccountRepository {   // ◄── implémente le PORT OUT

    private final AccountJpaRepository jpa;
    private final AccountMapper mapper;

    AccountPersistenceAdapter(AccountJpaRepository jpa, AccountMapper mapper) {
        this.jpa = jpa; this.mapper = mapper;
    }

    public Optional<Account> findById(AccountId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }
    public Account save(Account account) {
        return mapper.toDomain(jpa.save(mapper.toEntity(account)));
    }
    public Optional<Account> findActiveByCustomerId(CustomerId id) {
        return jpa.findByCustomerIdAndStatus(id.value(), "ACTIVE").map(mapper::toDomain);
    }
}

// L'entité JPA est SÉPARÉE du modèle domaine
@Entity @Table(name = "accounts")
class AccountJpaEntity {
    @Id private UUID id;
    private UUID customerId;
    private String iban;
    @Enumerated(EnumType.STRING) private OfferEntity offer;
    private BigDecimal balance;
    @Enumerated(EnumType.STRING) private AccountStatusEntity status;
    @Version private long version;             // verrou optimiste géré par JPA
}
```

> **Flux complet** : `TransferController` → `MakeTransferUseCase` (port) →
> `MakeTransferService` → `Account.debit()` (règles) + `AccountRepository` (port) →
> `AccountPersistenceAdapter` → BDD. Le métier n'a **jamais vu** Spring ni JPA.

---

## 7. Pourquoi des Value Objects ?

```java
// domain/vo/Money.java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.scale() > 2)        throw new IllegalArgumentException("max 2 décimales");
        if (amount.signum() < 0)       throw new IllegalArgumentException("montant négatif");
    }
    public boolean isLessThan(Money o) { return amount.compareTo(o.amount) < 0; }
    public Money minus(Money o)        { return new Money(amount.subtract(o.amount), currency); }
}

// domain/vo/Iban.java  → valide format + clé MOD 97 à la construction
public record Iban(String value) {
    public Iban { if (!IbanValidator.isValid(value)) throw new InvalidIbanException(value); }
}
```

Avantages : impossible de manipuler un IBAN invalide ou un montant négatif → la validation
est **garantie par le type**, pas éparpillée dans des `if`.

---

## 8. Mapping & frontières (à savoir justifier)

| Couche | Représentation | Validé par |
|---|---|---|
| Web (adapter IN) | `TransferRequest` / `TransferResponse` (DTO) | Bean Validation |
| Application | `MakeTransferCommand` (record immuable) | — |
| Domaine | `Account`, `Money`, `Iban` (VO) | invariants métier |
| Persistence (adapter OUT) | `AccountJpaEntity` | contraintes BDD |

On ne fait **jamais** fuiter l'entité JPA jusqu'au controller, ni le DTO web jusqu'au
domaine. Les mappers (MapStruct) font la traduction aux frontières.

---

## 9. Testabilité (gros argument à l'oral)

- **Domaine** : tests unitaires purs, sans Spring (`Account.debit()` → `InsufficientFundsException`).
- **Use cases** : tests avec des **mocks des ports OUT** (Mockito) → rapides, aucune BDD.
- **Adapters OUT** : tests d'intégration avec **Testcontainers** (PostgreSQL réel).
- **Adapters IN** : `@WebMvcTest` en mockant le port IN.

```java
@Test
void virement_refuse_si_solde_insuffisant() {
    Account a = AccountFixtures.withBalance(Money.eur(100));
    assertThatThrownBy(() -> a.debit(Money.eur(500)))
        .isInstanceOf(InsufficientFundsException.class);
}
```

---

## 10. Ce qu'il faut dire à l'oral (et les pièges)

**Les phrases qui marquent :**
- « Le domaine ne dépend d'aucun framework, donc je peux le tester sans démarrer Spring. »
- « Les ports IN portent les use cases, les ports OUT expriment les besoins d'infrastructure ;
  c'est de l'inversion de dépendances. »
- « Le controller dépend de l'interface `MakeTransferUseCase`, pas de l'implémentation. »
- « J'ai trois modèles distincts : DTO web, command applicative, entité domaine, entité JPA. »

**Les pièges à éviter :**
- ❌ Mettre `@Entity` sur le modèle domaine (couplage JPA dans le cœur).
- ❌ Domaine anémique (entités sans comportement, tout dans des services).
- ❌ Retourner l'entité JPA directement dans la réponse REST.
- ❌ Que le domaine appelle Spring (`@Autowired` dans une entité).
- ❌ Confondre **hexagonal** (frontières par ports/adapters) et **n‑tiers** (couches techniques empilées).

**Différence rapide à connaître :**
Hexagonal ≈ Clean Architecture ≈ Onion : même règle de dépendance vers l'intérieur, même
domaine isolé. La nuance d'hexagonal, c'est l'accent mis sur **ports & adapters symétriques**
(côté driving et côté driven).

---

*Fichier compagnon de la spec fonctionnelle/technique easyBank. Idéal pour répondre à
« comment structureriez‑vous ce projet ? » pendant l'entretien.*
