# easyBank — Spécification Fonctionnelle & Technique

> Banque en ligne — Document de conception pour entretien technique Java
> Version 1.0 — Statut : Draft

---

## 1. Contexte & périmètre

easyBank est une **banque 100 % en ligne**. L'application web permet à un client de :
souscrire à un compte courant, choisir une offre, commander une carte, gérer des
bénéficiaires, émettre des virements, récupérer son RIB, télécharger ses relevés et
clôturer son compte.

Le périmètre du POC couvre la **gestion du compte courant** (pas de crédit, épargne,
ni titres). Les montants sont en **EUR**.

---

## 2. Identité bancaire de easyBank

### 2.1 BIC

Format ISO 9362 (8 ou 11 caractères) : `BBBB` (banque) + `CC` (pays) + `LL` (localité) + `BBB` (agence, optionnel).

| Élément     | Valeur | Signification     |
|-------------|--------|-------------------|
| Code banque | `EASY` | easyBank          |
| Pays        | `FR`   | France            |
| Localité    | `PP`   | Paris             |
| Agence      | `XXX`  | Siège (principal) |

> **BIC easyBank : `EASYFRPP`** (forme courte) / **`EASYFRPPXXX`** (forme longue).

### 2.2 Format IBAN (France — 27 caractères)

```
FR  kk   BBBBB   GGGGG   CCCCCCCCCCC   KK
│   │    │       │       │             └─ Clé RIB (2 chiffres)
│   │    │       │       └─ N° de compte (11 alphanum.)
│   │    │       └─ Code guichet (5 chiffres)
│   │    └─ Code banque (5 chiffres)
│   └─ Clé de contrôle IBAN (2 chiffres, ISO 7064 MOD 97-10)
└─ Code pays (ISO 3166)
```

| Champ        | Longueur | Valeur easyBank (fictive) |
|--------------|----------|---------------------------|
| Code banque  | 5        | `30100`                   |
| Code guichet | 5        | `00001`                   |
| N° de compte | 11       | généré par compte         |
| Clé RIB      | 2        | calculée (cf. §11.1)      |

> **Exemple d'IBAN valide** (vérifié MOD 97 = 1) :
> `FR76 3010 0000 0100 0123 4567 822`
>
> Codes banque/guichet fictifs : en production ils sont attribués par la Banque de France.

---

## 3. Domaine métier & identifiants

Tous les identifiants techniques sont des **UUID v4** (opaques, non devinables, générés côté serveur).

| Entité                     | ID technique           | Identifiant métier   |
|----------------------------|------------------------|----------------------|
| Customer (client)          | `customerId` : UUID    | e‑mail unique        |
| Account (compte courant)   | `accountId` : UUID     | IBAN                 |
| Beneficiary (bénéficiaire) | `beneficiaryId` : UUID | IBAN du bénéficiaire |
| Card (carte)               | `cardId` : UUID        | PAN masqué           |
| Transfer (virement)        | `transferId` : UUID    | —                    |
| Statement (relevé)         | `statementId` : UUID   | mois (AAAA‑MM)       |

> ⚠️ L'UUID identifie la ressource techniquement ; l'**IBAN** reste l'identifiant
> métier exposé au client. On ne fait jamais de virement « vers un UUID » mais vers un IBAN.

---

## 4. Offres

| Caractéristique              | Offre **EASY**       | Offre **PREMIUM**    |
|------------------------------|----------------------|----------------------|
| Cotisation                   | Gratuite             | Payante (mensuelle)  |
| Carte                        | Visa Classic — débit | Visa Premier — débit |
| Plafond paiement / 30 j      | 2 000 €              | 10 000 €             |
| Plafond retrait / 7 j        | 500 €                | 2 000 €              |
| Plafond virement / opération | 5 000 €              | 50 000 €             |
| Assurances / assistance      | Non                  | Oui                  |

Le **changement d'offre** est instantané ; les nouveaux plafonds s'appliquent
immédiatement, la carte existante n'est pas réémise (seuls les plafonds changent).

---

## 5. Règles de gestion (RG)

| #     | Règle                                                                                                |
|-------|------------------------------------------------------------------------------------------------------|
| RG‑01 | Un client doit être identifié (KYC simplifié) avant toute souscription.                              |
| RG‑02 | **Un client ne peut détenir qu'UN SEUL compte courant ACTIF à la fois.**                             |
| RG‑03 | Il peut souscrire un nouveau compte **uniquement après clôture** du précédent (historique conservé). |
| RG‑04 | À la souscription, le client choisit une offre (`EASY` ou `PREMIUM`).                                |
| RG‑05 | Une carte ne peut être générée que pour un compte `ACTIVE`.                                          |
| RG‑06 | Le type de carte est déterminé par l'offre du compte au moment de l'émission.                        |
| RG‑07 | Un virement est refusé si solde insuffisant ou plafond dépassé.                                      |
| RG‑08 | Un virement externe cible obligatoirement un **bénéficiaire enregistré** (IBAN connu).               |
| RG‑09 | Un IBAN/BIC de bénéficiaire est validé (format + clé) avant enregistrement.                          |
| RG‑10 | La clôture est refusée si le solde ≠ 0 (régulariser d'abord).                                        |
| RG‑11 | La clôture désactive les cartes et bénéficiaires associés.                                           |
| RG‑12 | Un compte `CLOSED` est immuable (lecture seule : RIB historique, relevés).                           |
| RG‑13 | Le relevé est mensuel, généré à la demande, au format JSON et PDF.                                   |
| RG‑14 | Toutes les écritures sont en EUR ; pas de découvert autorisé sur ce POC.                             |

---

## 6. Cas d'usage fonctionnels

### UC‑01 — Souscription à un compte courant

- **Acteur** : client identifié sans compte actif.
- **Entrées** : `customerId`, offre choisie.
- **Règles** : RG‑02, RG‑03, RG‑04.
- **Effets** : création du compte (`ACTIVE`), génération IBAN + clé RIB, solde = 0.
- **Sortie** : compte créé avec IBAN.

### UC‑02 — Ajouter un bénéficiaire

- **Entrées** : nom, IBAN, BIC du bénéficiaire.
- **Règles** : RG‑09 (validation IBAN/BIC).
- **Effets** : bénéficiaire rattaché au compte.

### UC‑03 — Faire un virement

- **Types** : interne (compte→compte easyBank) ou externe (vers bénéficiaire).
- **Règles** : RG‑07, RG‑08, plafonds offre.
- **Effets** : débit émetteur, crédit destinataire, 2 écritures (transaction), statut `EXECUTED`.
- **Idempotence** : clé `Idempotency-Key` obligatoire (évite le double virement).

### UC‑04 — Générer une carte bancaire

- **Entrées** : `accountId`.
- **Règles** : RG‑05, RG‑06.
- **Effets** : carte créée (PAN, expiration, type selon offre), statut `ACTIVE`.
- **Sécurité** : CVV non stocké en clair, PAN masqué en réponse (`**** **** **** 1234`).

### UC‑05 — Changement d'offre

- **Entrées** : nouvelle offre.
- **Effets** : mise à jour de l'offre et des plafonds (RG‑06 pour future carte).

### UC‑06 — Clôture de compte

- **Règles** : RG‑10, RG‑11, RG‑12.
- **Effets** : statut `CLOSED`, cartes/bénéficiaires désactivés ; relevés et RIB restent consultables.

### UC‑07 — Récupérer le RIB

- **Sortie** : document RIB (titulaire, IBAN, BIC, domiciliation) en JSON et PDF.

### UC‑08 — Relevé bancaire mensuel

- **Entrées** : mois (`AAAA‑MM`).
- **Sortie** : liste des opérations + solde initial/final, en JSON et **PDF**.

---

## 7. Modèle de données (logique)

```
Customer 1 ───< Account (1 ACTIVE max) 1 ───< Card
                       │
                       ├───< Beneficiary
                       ├───< Transfer ───< Transaction (entry)
                       └───< Statement
```

### Énumérations

| Enum                   | Valeurs                           |
|------------------------|-----------------------------------|
| `AccountStatus`        | `ACTIVE`, `CLOSED`                |
| `Offer`                | `EASY`, `PREMIUM`                 |
| `CardType`             | `VISA_CLASSIC`, `VISA_PREMIER`    |
| `CardStatus`           | `ACTIVE`, `BLOCKED`, `EXPIRED`    |
| `TransferStatus`       | `PENDING`, `EXECUTED`, `REJECTED` |
| `TransferType`         | `INTERNAL`, `EXTERNAL`            |
| `TransactionDirection` | `DEBIT`, `CREDIT`                 |

### Attributs principaux

**Customer** : `customerId`, `firstName`, `lastName`, `email`, `phone`, `createdAt`.
**Account** : `accountId`, `customerId`, `iban`, `bic`, `offer`, `balance`, `currency`, `status`, `openedAt`,`closedAt`,
`version`.
**Card** : `cardId`, `accountId`, `maskedPan`, `type`, `status`, `expiryDate`.
**Beneficiary** : `beneficiaryId`, `accountId`, `name`, `iban`, `bic`, `createdAt`.
**Transfer** : `transferId`, `sourceAccountId`, `targetIban`, `amount`, `currency`, `label`, `type`, `status`,
`executedAt`.
**Transaction** : `transactionId`, `accountId`, `transferId`, `direction`, `amount`, `balanceAfter`, `valueDate`.

> Note conception : `version` (Long) sur `Account` pour le **verrouillage optimiste**
> (`@Version` JPA) — protège la cohérence du solde en concurrence.

---

## 8. Spécification technique des API REST

### 8.1 Conventions générales

| Sujet       | Choix                                                                          |
|-------------|--------------------------------------------------------------------------------|
| Style       | REST, ressources nommées au pluriel                                            |
| Base URL    | `/api/v1`                                                                      |
| Format      | JSON (`application/json`) ; PDF (`application/pdf`) pour documents             |
| Auth        | `Authorization: Bearer <JWT>` (OAuth2)                                         |
| Idempotence | En‑tête `Idempotency-Key` sur les POST monétaires (virements)                  |
| Pagination  | `?page=0&size=20` ; réponse enveloppée `{content, page, size, totalElements}`  |
| Tri         | `?sort=valueDate,desc`                                                         |
| Dates       | ISO‑8601 (`2026-05-31T10:15:30Z`)                                              |
| Montants    | Décimal en EUR (`1500.00`), jamais en float binaire côté métier (`BigDecimal`) |
| Erreurs     | RFC 7807 `application/problem+json`                                            |
| Versioning  | Préfixe d'URL `/v1`                                                            |

### 8.2 Format d'erreur (RFC 7807)

```json
{
  "type": "https://api.easybank.fr/errors/insufficient-funds",
  "title": "Solde insuffisant",
  "status": 422,
  "detail": "Le solde (120.00 EUR) est insuffisant pour un virement de 500.00 EUR",
  "instance": "/api/v1/accounts/3f.../transfers",
  "code": "INSUFFICIENT_FUNDS",
  "timestamp": "2026-06-17T09:30:00Z"
}
```

### 8.3 Catalogue des endpoints

| #  | Méthode  | Endpoint                                              | Description                  | UC    |
|----|----------|-------------------------------------------------------|------------------------------|-------|
| 1  | `POST`   | `/customers`                                          | Créer un client              | —     |
| 2  | `GET`    | `/customers/{customerId}`                             | Détail client                | —     |
| 3  | `POST`   | `/customers/{customerId}/accounts`                    | Souscrire un compte courant  | UC‑01 |
| 4  | `GET`    | `/customers/{customerId}/accounts`                    | Lister les comptes du client | —     |
| 5  | `GET`    | `/accounts/{accountId}`                               | Détail d'un compte           | —     |
| 6  | `PATCH`  | `/accounts/{accountId}/offer`                         | Changer d'offre              | UC‑05 |
| 7  | `POST`   | `/accounts/{accountId}/closure`                       | Clôturer le compte           | UC‑06 |
| 8  | `POST`   | `/accounts/{accountId}/cards`                         | Générer une carte            | UC‑04 |
| 9  | `GET`    | `/accounts/{accountId}/cards`                         | Lister les cartes            | —     |
| 10 | `POST`   | `/accounts/{accountId}/beneficiaries`                 | Ajouter un bénéficiaire      | UC‑02 |
| 11 | `GET`    | `/accounts/{accountId}/beneficiaries`                 | Lister les bénéficiaires     | —     |
| 12 | `DELETE` | `/accounts/{accountId}/beneficiaries/{beneficiaryId}` | Supprimer un bénéficiaire    | —     |
| 13 | `POST`   | `/accounts/{accountId}/transfers`                     | Émettre un virement          | UC‑03 |
| 14 | `GET`    | `/accounts/{accountId}/transfers`                     | Historique des virements     | —     |
| 15 | `GET`    | `/accounts/{accountId}/rib`                           | RIB (JSON)                   | UC‑07 |
| 16 | `GET`    | `/accounts/{accountId}/rib.pdf`                       | RIB (PDF)                    | UC‑07 |
| 17 | `GET`    | `/accounts/{accountId}/statements?month=AAAA-MM`      | Relevé mensuel (JSON)        | UC‑08 |
| 18 | `GET`    | `/accounts/{accountId}/statements/{AAAA-MM}.pdf`      | Relevé mensuel (PDF)         | UC‑08 |

---

## 9. Contrats d'interface détaillés

### 9.1 Créer un client — `POST /customers`

**Requête**

```json
{
  "firstName": "Ahmed",
  "lastName": "Benali",
  "email": "ahmed.benali@example.com",
  "phone": "+33612345678"
}
```

**Réponse `201 Created`** — `Location: /api/v1/customers/{customerId}`

```json
{
  "customerId": "2c4a1d90-7e6b-4b6a-9f2d-1a2b3c4d5e6f",
  "firstName": "Ahmed",
  "lastName": "Benali",
  "email": "ahmed.benali@example.com",
  "createdAt": "2026-06-17T09:00:00Z"
}
```

**Erreurs** : `400` (validation), `409 EMAIL_ALREADY_USED`.

---

### 9.2 Souscrire un compte — `POST /customers/{customerId}/accounts`

**Requête**

```json
{
  "offer": "EASY"
}
```

**Réponse `201 Created`**

```json
{
  "accountId": "a1b2c3d4-0000-4000-8000-000000000001",
  "customerId": "2c4a1d90-7e6b-4b6a-9f2d-1a2b3c4d5e6f",
  "iban": "FR7630100000010001234567822",
  "bic": "EASYFRPP",
  "offer": "EASY",
  "balance": 0.00,
  "currency": "EUR",
  "status": "ACTIVE",
  "openedAt": "2026-06-17T09:05:00Z"
}
```

**Erreurs** : `409 ACTIVE_ACCOUNT_EXISTS` (RG‑02), `400 INVALID_OFFER`.

---

### 9.3 Ajouter un bénéficiaire — `POST /accounts/{accountId}/beneficiaries`

**Requête**

```json
{
  "name": "Sara Lopez",
  "iban": "FR1420041010050500013M02606",
  "bic": "PSSTFRPPXXX"
}
```

**Réponse `201 Created`**

```json
{
  "beneficiaryId": "be11f1c1-0000-4000-8000-000000000099",
  "name": "Sara Lopez",
  "iban": "FR1420041010050500013M02606",
  "bic": "PSSTFRPPXXX",
  "createdAt": "2026-06-17T09:10:00Z"
}
```

**Erreurs** : `400 INVALID_IBAN` / `INVALID_BIC`, `409 BENEFICIARY_ALREADY_EXISTS`.

---

### 9.4 Faire un virement — `POST /accounts/{accountId}/transfers`

**En‑têtes** : `Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d`

**Requête**

```json
{
  "targetIban": "FR1420041010050500013M02606",
  "amount": 250.00,
  "currency": "EUR",
  "label": "Remboursement déjeuner"
}
```

**Réponse `201 Created`**

```json
{
  "transferId": "7f0e2b10-0000-4000-8000-0000000000aa",
  "sourceAccountId": "a1b2c3d4-0000-4000-8000-000000000001",
  "targetIban": "FR1420041010050500013M02606",
  "amount": 250.00,
  "currency": "EUR",
  "type": "EXTERNAL",
  "status": "EXECUTED",
  "balanceAfter": 1250.00,
  "executedAt": "2026-06-17T09:15:00Z"
}
```

**Erreurs** :

- `422 INSUFFICIENT_FUNDS` (RG‑07)
- `422 LIMIT_EXCEEDED` (plafond offre)
- `404 BENEFICIARY_NOT_FOUND` (RG‑08)
- `409 ACCOUNT_CLOSED`
- `200 OK` (replay idempotent : renvoie le virement déjà exécuté)

---

### 9.5 Générer une carte — `POST /accounts/{accountId}/cards`

**Réponse `201 Created`**

```json
{
  "cardId": "ca7d0000-0000-4000-8000-0000000000c1",
  "maskedPan": "**** **** **** 1234",
  "type": "VISA_CLASSIC",
  "status": "ACTIVE",
  "expiryDate": "2030-06"
}
```

> Le PAN complet et le CVV ne sont **jamais** renvoyés par l'API (PCI‑DSS).
**Erreurs** : `409 ACCOUNT_CLOSED` (RG‑05).

---

### 9.6 Changer d'offre — `PATCH /accounts/{accountId}/offer`

**Requête**

```json
{
  "offer": "PREMIUM"
}
```

**Réponse `200 OK`** — compte mis à jour (nouveaux plafonds appliqués).
**Erreurs** : `400 INVALID_OFFER`, `409 SAME_OFFER`, `409 ACCOUNT_CLOSED`.

---

### 9.7 Clôturer le compte — `POST /accounts/{accountId}/closure`

**Réponse `200 OK`**

```json
{
  "accountId": "a1b2c3d4-...",
  "status": "CLOSED",
  "closedAt": "2026-06-17T09:20:00Z"
}
```

**Erreurs** : `422 NON_ZERO_BALANCE` (RG‑10), `409 ALREADY_CLOSED`.

---

### 9.8 RIB — `GET /accounts/{accountId}/rib`

**Réponse `200 OK`**

```json
{
  "holder": "Ahmed Benali",
  "iban": "FR7630100000010001234567822",
  "bic": "EASYFRPP",
  "bankName": "easyBank",
  "domiciliation": "EASYBANK PARIS"
}
```

> Variante PDF : `GET /accounts/{accountId}/rib.pdf` → `Content-Type: application/pdf`.

---

### 9.9 Relevé mensuel — `GET /accounts/{accountId}/statements?month=2026-05`

**Réponse `200 OK`**

```json
{
  "accountId": "a1b2c3d4-...",
  "month": "2026-05",
  "openingBalance": 1000.00,
  "closingBalance": 1250.00,
  "operations": [
    {
      "transactionId": "tx-001",
      "valueDate": "2026-05-12",
      "label": "Virement reçu - Salaire",
      "direction": "CREDIT",
      "amount": 500.00,
      "balanceAfter": 1500.00
    },
    {
      "transactionId": "tx-002",
      "valueDate": "2026-05-20",
      "label": "Virement émis - Sara Lopez",
      "direction": "DEBIT",
      "amount": 250.00,
      "balanceAfter": 1250.00
    }
  ]
}
```

> Variante PDF : `GET /accounts/{accountId}/statements/2026-05.pdf`.
**Erreurs** : `400 INVALID_MONTH`, `404 NO_DATA_FOR_PERIOD`.

---

## 10. Codes d'erreur métier (synthèse)

| Code                           | HTTP | Sens                                     |
|--------------------------------|------|------------------------------------------|
| `EMAIL_ALREADY_USED`           | 409  | E‑mail déjà rattaché à un client         |
| `ACTIVE_ACCOUNT_EXISTS`        | 409  | Le client a déjà un compte actif (RG‑02) |
| `INVALID_OFFER`                | 400  | Offre inconnue                           |
| `SAME_OFFER`                   | 409  | Offre identique à l'actuelle             |
| `INVALID_IBAN` / `INVALID_BIC` | 400  | Format/clé invalide                      |
| `BENEFICIARY_NOT_FOUND`        | 404  | Bénéficiaire absent                      |
| `INSUFFICIENT_FUNDS`           | 422  | Solde insuffisant                        |
| `LIMIT_EXCEEDED`               | 422  | Plafond de l'offre dépassé               |
| `ACCOUNT_CLOSED`               | 409  | Opération interdite sur compte clôturé   |
| `NON_ZERO_BALANCE`             | 422  | Clôture impossible, solde ≠ 0            |
| `NO_DATA_FOR_PERIOD`           | 404  | Aucune opération sur le mois             |

---

## 11. Annexes — algorithmes

### 11.1 Clé RIB (contrôle national)

```
cleRIB = 97 − ( (89 × banque + 15 × guichet + 3 × compte) mod 97 )
```

Les lettres éventuelles du n° de compte sont converties (A=1 … Z selon table AFNOR)
avant calcul. Exemple easyBank : banque=30100, guichet=00001, compte=00012345678 → **clé = 22**.

### 11.2 Clé de contrôle IBAN (ISO 7064, MOD 97‑10)

1. Déplacer les 4 premiers caractères (`FR` + `00`) à la fin du BBAN.
2. Remplacer chaque lettre par sa valeur (A=10 … Z=35) → `F=15`, `R=27`.
3. `cle = 98 − (nombre mod 97)`.
4. Validation : un IBAN est correct si `IBAN_réarrangé mod 97 == 1`.

Exemple vérifié : `FR7630100000010001234567822` → `mod 97 = 1` ✅

---

## 12. Choix techniques suggérés (pour l'oral)

- **Stack** : Spring Boot 3 (Java 21), Spring Web, Spring Data JPA, Bean Validation,
  PostgreSQL, Flyway (migrations), springdoc‑openapi (Swagger UI), iText/OpenPDF (PDF).
- **Architecture** : hexagonale / en couches (Controller → Service → Repository),
  DTO ≠ Entité (mapping MapStruct), `BigDecimal` pour la monnaie.
- **Cohérence** : `@Transactional` sur le virement (débit + crédit atomiques),
  `@Version` (verrou optimiste) sur `Account`.
- **Sécurité** : OAuth2/JWT, masquage PAN, pas de CVV stocké, validation IBAN/BIC.
- **Idempotence** : table `idempotency_keys` pour rejouer un POST virement sans doublon.
- **Tests** : unitaires (services + algos IBAN/clé RIB) + intégration (`@SpringBootTest`,
  Testcontainers PostgreSQL) + cas limites (solde insuffisant, plafond, compte clôturé).
- **Qualité** : OpenAPI comme contrat, codes d'erreur normalisés (RFC 7807).

---

*Points que l'examinateur appréciera probablement : la règle RG‑02 (un seul compte actif),
l'idempotence des virements, l'atomicité débit/crédit, le verrou optimiste, et la séparation
nette identifiant technique (UUID) / identifiant métier (IBAN).*