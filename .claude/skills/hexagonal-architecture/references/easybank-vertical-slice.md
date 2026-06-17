# Tranche verticale de référence — le virement

Squelette exact à reproduire pour un use case en archi hexagonale. Suit la règle de
dépendance (du cœur vers l'extérieur). Reproduis ce pattern pour les autres use cases
(souscription, clôture, émission de carte, etc.).

## 1. Domaine — entité riche (les règles vivent ici)

```java
// domain/model/Account.java
public class Account {
    private final AccountId id;
    private final CustomerId customerId;
    private final Iban iban;
    private Offer offer;
    private Money balance;
    private AccountStatus status;
    private long version;

    public void debit(Money amount) {
        if (status != AccountStatus.ACTIVE) throw new AccountClosedException(id);
        if (balance.isLessThan(amount)) throw new InsufficientFundsException(id, balance, amount);
        if (amount.isGreaterThan(offer.transferLimit()))
            throw new LimitExceededException(offer, amount);
        this.balance = balance.minus(amount);
    }

    public void close() {
        if (!balance.isZero()) throw new NonZeroBalanceException(id);
        this.status = AccountStatus.CLOSED;
    }
    // getters ; pas de setter public sur balance/status
}
```

## 2. Value Object monétaire (auto-validant)

```java
// domain/vo/Money.java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.scale() > 2) throw new IllegalArgumentException("max 2 décimales");
        if (amount.signum() < 0) throw new IllegalArgumentException("montant négatif");
    }

    public boolean isLessThan(Money o) {
        return amount.compareTo(o.amount) < 0;
    }

    public boolean isGreaterThan(Money o) {
        return amount.compareTo(o.amount) > 0;
    }

    public Money minus(Money o) {
        return new Money(amount.subtract(o.amount), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }
}
```

## 3. Port IN + Command

```java
// application/port/in/MakeTransferUseCase.java
public interface MakeTransferUseCase {
    TransferResult makeTransfer(MakeTransferCommand command);
}

// application/port/in/MakeTransferCommand.java
public record MakeTransferCommand(
        AccountId sourceAccountId,
        Iban targetIban,
        Money amount,
        String label,
        String idempotencyKey) {
}
```

## 4. Ports OUT

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

## 5. Service applicatif (orchestration + transaction)

```java
// application/service/MakeTransferService.java
@Service
public class MakeTransferService implements MakeTransferUseCase {

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransferRepository transferRepository;

    public MakeTransferService(AccountRepository a, BeneficiaryRepository b, TransferRepository t) {
        this.accountRepository = a;
        this.beneficiaryRepository = b;
        this.transferRepository = t;
    }

    @Override
    @Transactional
    public TransferResult makeTransfer(MakeTransferCommand cmd) {
        Account source = accountRepository.findById(cmd.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(cmd.sourceAccountId()));

        // RG-08 : virement externe ⇒ bénéficiaire enregistré
        beneficiaryRepository.findByAccountIdAndIban(source.getId(), cmd.targetIban())
                .orElseThrow(() -> new BeneficiaryNotFoundException(cmd.targetIban()));

        source.debit(cmd.amount());          // toutes les règles sont DANS le domaine
        accountRepository.save(source);

        Transfer transfer = Transfer.execute(
                source.getId(), cmd.targetIban(), cmd.amount(), cmd.label());
        return TransferResult.from(transferRepository.save(transfer));
    }
}
```

## 6. Adapter IN (REST) — dépend du PORT, pas du service concret

```java
// infrastructure/adapter/in/web/TransferController.java
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/transfers")
class TransferController {

    private final MakeTransferUseCase makeTransfer;   // ◄── le PORT IN

    TransferController(MakeTransferUseCase makeTransfer) {
        this.makeTransfer = makeTransfer;
    }

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

## 7. Adapter OUT (persistence) — implémente le port, entité JPA séparée

```java
// infrastructure/adapter/out/persistence/AccountPersistenceAdapter.java
@Component
class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository jpa;
    private final AccountMapper mapper;

    AccountPersistenceAdapter(AccountJpaRepository jpa, AccountMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
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

// infrastructure/adapter/out/persistence/AccountJpaEntity.java
@Entity
@Table(name = "accounts")
class AccountJpaEntity {
    @Id
    private UUID id;
    private UUID customerId;
    private String iban;
    @Enumerated(EnumType.STRING)
    private OfferEntity offer;
    private BigDecimal balance;
    @Enumerated(EnumType.STRING)
    private AccountStatusEntity status;
    @Version
    private long version;             // verrou optimiste
}
```

## Flux complet

`TransferController` → `MakeTransferUseCase` (port) → `MakeTransferService` →
`Account.debit()` (règles) + `AccountRepository` (port) → `AccountPersistenceAdapter` → BDD.

Le métier n'a jamais vu Spring ni JPA : il reste testable sans démarrer le framework.