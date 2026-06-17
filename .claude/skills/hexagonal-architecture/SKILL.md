---
name: hexagonal-architecture
description: >
  Conventions d'architecture hexagonale (ports & adapters) pour le projet easyBank
  (Java 25 / Spring Boot 4). Utilise IMPÉRATIVEMENT ce skill dès que tu crées, modifies
  ou organises du code backend : controller REST, service, use case, repository, entité,
  value object, mapper, ou dès qu'il faut décider DANS QUEL package mettre une classe.
  Déclenche-le aussi quand l'utilisateur parle de "compte", "virement", "carte",
  "bénéficiaire", "clôture", "offre", de couches, de ports/adapters, ou demande "où mettre"
  une classe. En cas de doute sur le placement ou le sens des dépendances, consulte ce skill
  AVANT d'écrire le code.
---

# Architecture hexagonale — easyBank

Ce projet suit l'architecture hexagonale (ports & adapters). Respecte ces conventions
pour TOUTE création ou modification de code backend.

## Règle de dépendance (non négociable)

Les dépendances pointent **toujours vers l'intérieur** :

```
infrastructure  ──►  application  ──►  domain
```

- `domain` ne dépend de **rien** (ni Spring, ni JPA, ni Jackson). Java pur.
- `application` ne dépend **que** de `domain`.
- `infrastructure` dépend de `application` (elle implémente ou appelle ses ports).

Si tu es tenté de faire pointer une dépendance vers l'extérieur (ex. importer une classe
`infrastructure` depuis `domain`), c'est une erreur d'architecture : arrête-toi et inverse
la dépendance via un port.

## Structure de packages (racine : `com.easybank`)

| Package | Contenu | Dépendances autorisées |
|---|---|---|
| `domain.model` | Entités riches (`Account`, `Transfer`, `Card`, `Beneficiary`, `Customer`) | rien |
| `domain.vo` | Value Objects immuables (`Iban`, `Bic`, `Money`, `Offer`, `AccountId`…) | rien |
| `domain.exception` | Exceptions métier | rien |
| `application.port.in` | Ports IN = une interface par use case (+ records `Command`) | `domain` |
| `application.port.out` | Ports OUT = besoins du métier (`AccountRepository`, `StatementPdfGenerator`…) | `domain` |
| `application.service` | Implémentations des use cases (orchestration) | `domain`, `application.port` |
| `infrastructure.adapter.in.web` | Controllers REST + DTO + mappers + `GlobalExceptionHandler` | `application.port.in` |
| `infrastructure.adapter.out.persistence` | Entités JPA + repositories Spring Data + adapters | `application.port.out` |
| `infrastructure.adapter.out.pdf` | Génération PDF (relevé, RIB) | `application.port.out` |
| `infrastructure.config` | Configuration Spring, beans | tout |

## Conventions de nommage

- Port IN : `<Action>UseCase` (ex. `MakeTransferUseCase`). Une interface = un use case.
- Entrée d'un use case : un record immuable `<Action>Command`.
- Port OUT : nom métier sans techno (`AccountRepository`, pas `AccountJpaDao`).
- Implémentation d'un port OUT : `<Nom>...Adapter` (ex. `AccountPersistenceAdapter`).
- Entité JPA : suffixe `JpaEntity` (ex. `AccountJpaEntity`) — **distincte** du modèle domaine.
- DTO web : `<Action>Request` / `<Action>Response`. Jamais l'entité directement exposée.

## Règles à appliquer systématiquement

1. **Domaine pur** : aucune annotation `@Entity`, `@Component`, `@Autowired`,
   `@JsonProperty` sur les classes de `domain`. Si tu as besoin de persistance, crée une
   `*JpaEntity` séparée dans `infrastructure.adapter.out.persistence` + un mapper.
2. **Entités riches** : mets les règles métier DANS les entités du domaine
   (`account.debit(money)` lève `InsufficientFundsException`), pas dans des services anémiques.
3. **Le controller dépend du PORT IN** (l'interface `...UseCase`), jamais de la classe
   `...Service` concrète.
4. **Trois représentations distinctes**, reliées par des mappers (MapStruct) aux frontières :
   DTO web ↔ Command/Domain ↔ JpaEntity. Ne laisse jamais fuiter une `*JpaEntity` jusqu'au
   controller, ni un DTO web jusqu'au domaine.
5. **Transaction dans l'application** : annote le service de use case avec `@Transactional`
   pour les opérations qui modifient l'état (un virement = débit + écriture, atomique).
6. **Argent** : utilise toujours le Value Object `Money` (basé sur `BigDecimal`), jamais
   `double` ni `float`.
7. **Identifiants** : UUID (Value Objects `AccountId`, `CustomerId`, `BeneficiaryId`) côté
   technique ; l'IBAN reste l'identifiant métier exposé.
8. **Verrou optimiste** : champ `@Version` sur `AccountJpaEntity` (cohérence du solde en
   concurrence).
9. **Erreurs** : les exceptions métier (`domain.exception`) sont traduites en réponses
   `application/problem+json` (RFC 7807) dans le `GlobalExceptionHandler` de la couche web.

## Pièges à ne jamais commettre

- ❌ `@Entity` sur une classe de `domain.model`.
- ❌ Domaine anémique (getters/setters sans comportement, logique dans le service).
- ❌ Retourner une `*JpaEntity` ou un objet domaine directement dans une réponse REST.
- ❌ Importer une classe `infrastructure` depuis `domain` ou `application`.
- ❌ Confondre hexagonal (frontières par ports/adapters) et simple empilement n-tiers.

## Quand tu crées un nouveau use case

Suis toujours cette séquence (du cœur vers l'extérieur) :
1. Ajoute/complète l'entité et ses règles dans `domain`.
2. Crée le port IN `<Action>UseCase` + le record `<Action>Command` dans `application.port.in`.
3. Crée/complète les ports OUT nécessaires dans `application.port.out`.
4. Implémente le service `<Action>Service` dans `application.service` (`@Transactional` si écriture).
5. Crée l'adapter IN (controller + DTO + mapper) dans `infrastructure.adapter.in.web`.
6. Implémente les adapters OUT (persistence, pdf…) dans `infrastructure.adapter.out.*`.

## Exemple de référence

Pour un exemple complet et commenté d'une tranche verticale (le virement, du controller
jusqu'à la base, avec entité riche, ports, service et adapters), lis :
`references/easybank-vertical-slice.md`.
Consulte-le quand tu implémentes un use case et que tu veux le squelette exact à reproduire.