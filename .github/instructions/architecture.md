# Architecture Guidelines

## Architectural Approach & Core Milestone

The project follows a **Clean Architecture / Ports & Adapters (Hexagonal Architecture)** design combined with **Domain-Driven Design (DDD)** and **CQRS (Command Query Responsibility Segregation)**.

The Core backend milestone was completed and tagged at:

`milestone/core-mvp-complete`

### Core Milestone Scope & System Boundaries

The current completed Core consists of:

```text
Domain
   ↑
Application
   ↑
Ports
   ↑
Infrastructure Adapters (JDBC)
   ↑
SQLite Persistence
```

### Components Status Summary

* **Core Domain**: COMPLETE
* **Application Layer**: COMPLETE
* **Infrastructure / Persistence for Core**: COMPLETE
* **Core Integration Boundary**: ESTABLISHED
* **Entry / Delivery Layer (REST API / Controllers)**: FOUNDATION IN PLACE (Spring Boot bootstrap, configuration, error mapping, single-user mode, `Clock`, and strict JSON; endpoints exist for accounts, categories, operations (E3), obligations (E4: lifecycle, occurrences, and queries), and the dashboard (E5); the entry layer plan (E1-E5, E2b, E4c) is complete; outside Core milestone)
* **iOS Application**: PENDING (outside Core milestone)
* **Complete MVP Product**: PENDING

### Future Architectural Layering

When the full product is integrated, the architecture will follow:

```text
iOS Frontend
     ↓
Entry / Delivery Layer
     ↓
       Core
```

*The entry layer uses Spring Boot 4.1.1 (Spring MVC). No iOS architecture/framework has been selected yet. The Core boundary remains delivery-agnostic.*

---

## Domain Layer

The Domain layer (`com.rauldev.personalfinance.domain`) contains business entities, aggregate roots, value objects, domain enums, domain exceptions (`BusinessRuleViolationException`, `BusinessRuleCode`), domain invariants, obligations (`Obligation`, `Recurrence`, `OccurrenceResolution`), and the pure `CommitmentCalculator` domain service (persistence ports and JDBC adapters exist; the lifecycle use cases `CreateObligation`, `ModifyObligation`, and `ArchiveObligation` exist (step 3a); the skip use cases `SkipOccurrence`, `SkipOverdueOccurrences`, and `ReopenOccurrence` exist (step 3b); the pay use case `PayOccurrence` exists (step 3c), so step 3 is complete; the dashboard read side (step 4: `GetDashboard`) exists, exposed by `GET /api/v1/dashboard`; see [`docs/obligations-core-proposal.md`](../../docs/obligations-core-proposal.md)).

It is completely independent of frameworks, databases, JDBC, and delivery mechanisms.

---

## Application Layer

The Application layer (`com.rauldev.personalfinance.application`) contains:

* Command Use Cases: `CreateAccount`, `ModifyAccount`, `CreateCategory`, `RegisterIncome`, `RegisterExpense`, `RegisterTransfer`, `CancelOperation`, `CreateObligation`, `ModifyObligation`, `ArchiveObligation`, `SkipOccurrence`, `SkipOverdueOccurrences`, `ReopenOccurrence`, `PayOccurrence`.
* Shared component: `ExpenseRegistration` (no transaction of its own; used by `RegisterExpense` and `PayOccurrence`).
* Query Use Cases: `GetAccount`, `ListAccounts`, `ListCategories`, `ListObligations`, `GetObligation`, `GetOperationHistory`, `GetOperationDetails`, `GetDashboard`.
* Input / Output Ports: Repositories, Query Ports, Transaction Manager contract (`application.port.out`).
* DTOs, Commands, Queries, and Read Models (`application.readmodel`).

The Application layer depends on Domain. It defines interfaces for external infrastructure dependencies and manages application orchestration.

---

## Infrastructure Layer

The Infrastructure layer (`com.rauldev.personalfinance.infrastructure`) contains:

* Database Connection: `SQLiteConnectionProvider`.
* Persistence Errors: `CorruptedPersistedDataException` (thrown when a stored row cannot be mapped).
* Transaction Management: `TransactionConnectionHolder`, `JdbcTransactionManager`.
* Command Repositories: `JdbcAccountRepository`, `JdbcCategoryRepository`, `JdbcIncomeOperationRepository`, `JdbcExpenseOperationRepository`, `JdbcTransferOperationRepository`, `JdbcReversalRepository`, `JdbcUserRepository`, `JdbcObligationRepository`, `JdbcOccurrenceResolutionRepository`.
* Query Adapters: `JdbcAccountQueryAdapter`, `JdbcCategoryQueryAdapter`, `JdbcObligationQueryAdapter`, `JdbcFinancialOperationQueryAdapter`, `JdbcDashboardQueryAdapter`, `JdbcUserQueryAdapter` (implements `UserQueryPort`). `JdbcObligationQueryAdapter` joins the account and category names with `LEFT JOIN`s scoped to the obligation's user, so a cross-user reference leaves the name `NULL` and the row fails as `CorruptedPersistedDataException` (500) instead of being silently dropped.
* Unique-violation translation: `JdbcAccountRepository`, `JdbcCategoryRepository`, `JdbcObligationRepository`, and `JdbcOccurrenceResolutionRepository` depend on `ApplicationConstants` (shared messages) and on sqlite-jdbc's `SQLiteException` / `SQLiteErrorCode`, through the package-private helper `SqliteConstraintViolations`. They translate a `UNIQUE (user_id, name)` violation (accounts, categories, obligations) into `BusinessRuleViolationException` (`*_NAME_ALREADY_EXISTS`), and a `UNIQUE (obligation_id, due_date)` violation on `occurrence_resolutions` into `OCCURRENCE_ALREADY_RESOLVED`. The `UNIQUE (expense_id)`, `CHECK`, and foreign key violations are not translated. Detection is tied to those constraints in `schema.sql` (it matches the constraint's columns in the SQLite message), so changing a constraint requires updating the adapters.
* Schema migrations: `src/main/resources/db/migrations/` holds manual, additive scripts (numbered, e.g. `001-obligations-core.sql`) for existing databases. Each script runs as a single transaction and is applied once, by hand, with the application stopped. `schema.sql` always holds the full schema for new databases and tests.

---

## Entry Layer

The Entry layer (`com.rauldev.personalfinance.entry`) is the only layer that depends on Spring (Spring Boot 4.1.1, Spring MVC). It adapts HTTP to application use cases. Endpoints exist for accounts and categories, including their list endpoints (E2b), for financial operations (E3), for obligations (E4: lifecycle, occurrences, and queries), and for the dashboard (E5); see REST API below. The entry layer plan is complete; no entry branches remain planned.

* **Component scan**: `PersonalFinanceApplication` lives in `entry`, so scanning is limited to the entry layer. Domain, application, and infrastructure classes are never scanned.
* **Core wiring**: the Core is wired only through explicit `@Bean` methods in `entry.config` (`@Configuration(proxyBeanMethods = false)`, dependencies as method parameters, beans exposed as port interfaces). Configuration classes that exist now: `PersistenceConfiguration`, `SqliteProperties`, `SingleUserProperties`, `SecurityConfiguration`, `TimeProperties`, `ClockConfiguration`, `AccountUseCaseConfiguration`, `OperationUseCaseConfiguration`, `ObligationUseCaseConfiguration`, `DashboardUseCaseConfiguration`. `PersistenceConfiguration` holds the infrastructure beans (connection provider, `TransactionConnectionHolder`, `TransactionManager`, repositories, query adapters; currently `UserQueryPort`, `AccountRepository`, `CategoryRepository`, `AccountQueryPort`, `CategoryQueryPort`, `IncomeOperationRepository`, `ExpenseOperationRepository`, `TransferOperationRepository`, `ReversalRepository`, `ObligationRepository`, `OccurrenceResolutionRepository`, `ObligationQueryPort`, `FinancialOperationQueryPort`, and `DashboardQueryPort`; the rest are added when an endpoint needs them), all sharing the one `TransactionConnectionHolder` bean. Use cases are wired in per-feature configuration classes, added with their endpoints; `AccountUseCaseConfiguration` (`CreateAccount`, `ModifyAccount`, `GetAccount`, `ListAccounts`, `CreateCategory`, `ListCategories`) and `OperationUseCaseConfiguration` (`RegisterIncome`, `RegisterExpense`, `RegisterTransfer`, `CancelOperation` with the `Clock` bean from `ClockConfiguration`, `GetOperationHistory`, `GetOperationDetails`) and `ObligationUseCaseConfiguration` (`CreateObligation`, `ModifyObligation`, `ArchiveObligation`, `ListObligations`, `GetObligation`, `SkipOccurrence`, `SkipOverdueOccurrences`, `ReopenOccurrence`, `PayOccurrence`, all the commands with the `Clock` bean except `ReopenOccurrence`; it also exposes an `ExpenseRegistration` bean, only for `PayOccurrence`: `RegisterExpense` builds its own) and `DashboardUseCaseConfiguration` (`GetDashboard` with `DashboardQueryPort`, `TransactionManager`, and the `Clock` bean; a query, but it receives the `TransactionManager` because it runs all its reads in one transaction).
* **Configuration**: `personal-finance.single-user-id` (required, no default, strict UUID; see Current user), `personal-finance.time-zone` (default `America/Mexico_City`, validated; backs the `Clock` bean in `ClockConfiguration`), `spring.mvc.format.date=iso` (ISO-8601 dates in path and query), and strict Jackson (`fail-on-unknown-properties=true`, `accept-float-as-int=false`: unknown JSON properties and a float for an integer field answer 400). Numeric strings (e.g. `"1050"`) are still coerced to numbers by Jackson's default; this is known and accepted for now.
* **SQLite configuration**: `personal-finance.sqlite.url` is required, has no default, and must start with `jdbc:sqlite:`. A missing or invalid value fails startup. Beans do not open a connection at startup and the schema is not initialized: the database must already be initialized from `src/main/resources/db/schema.sql`.
* **No Spring persistence**: no `DataSource`, JPA, or `@Transactional`. Transactions belong to the use cases through `TransactionManager`.
* **Blocking Spring MVC**: the entry layer is servlet-based (blocking) because `JdbcTransactionManager` binds the connection to a `ThreadLocal`.
* **Current user**: controllers obtain the user id only from `entry.security.CurrentUserProvider`; never accept a user id from the request (path, query, header, or body). No authentication mechanism exists yet, so the only implementation is `ConfiguredSingleUserProvider` (single-user mode), built in `SecurityConfiguration`. It returns the UUID in the required property `personal-finance.single-user-id`, which has no default; a missing or invalid value fails startup. The user must already exist in `users`, inserted by hand when the database is initialized (lowercase UUID, see `CLAUDE.md` → Commands). Its existence is checked through `UserQueryPort` on first use (a positive result is cached), not at startup, and a missing user throws `SingleUserNotProvisionedException` (500 with a clear server log). Do not add any other fixed, default, or fake user, and do not create users from configuration. Real authentication will replace only the `CurrentUserProvider` bean.
* **Security note**: in single-user mode anyone who can reach the server port has full access. Do not expose it beyond your machine or LAN.
* **Web helpers** (`entry.web.common`): `MoneyCents`, `RequiredFields`, `IdResponse`, `TransferResponse` (the `{sourceAccount, targetAccount}` part shared by operation and dashboard responses), and the `*SummaryResponse` DTOs shared by the web DTOs.
* **Controllers** (`entry.web`): call only use cases or application query ports; use web DTOs mapped to commands/queries and from read models (never serialize read models or domain objects); translate errors to RFC 9457 problem details in `entry.web.error` (see the HTTP mapping in [`application.md`](./application.md) → Error Handling).
* **REST API** (packages `entry.web.<feature>`, e.g. `entry.web.account`, `entry.web.category`):
  * Base path `/api/v1`; JSON in camelCase; ids are UUIDs in the path; dates are ISO-8601; money is an integer number of cents in a field with a `...Cents` suffix (`MoneyCents`); enums are uppercase strings; nulls are serialized as `null`.
  * Errors are `application/problem+json`; a 409 carries the stable `code`.
  * Creates answer `201` with `{id}` (`IdResponse`) and a `Location` header only where a GET for the resource exists; actions (modify, archive, ...) answer `204`.
  * Request DTOs are records with wrapper types and `toCommand(userId, …path ids)` (e.g. `ModifyAccountRequest.toCommand(userId, accountId)`); presence is checked with `RequiredFields` (a missing field answers 400 `Field 'x' is required`) and value rules stay in the Core. Request DTOs may bind domain enums directly (e.g. `CreateCategoryRequest.type` is a `CategoryType`); an unknown value answers 400 while the body is read.
  * Response DTOs are records with a static `from(readModel)`. Enums are serialized from `name()` as a `String` (e.g. `AccountResponse.status`).
  * Polymorphic responses are a sealed interface of records with an explicit `type` String component (e.g. `AttentionItemResponse`), mapped with an exhaustive `switch` over the sealed read model (no `default`). Do not use Jackson polymorphism annotations (`@JsonTypeInfo`/`@JsonSubTypes`): Jackson serializes each element by its runtime class.
  * The account, category, and obligation list endpoints answer a bare JSON array (`[]` when empty), unpaginated, with all statuses ordered by name then id; other list endpoints define their own shape and order (e.g. the paginated operations history `GET /api/v1/operations`, see below) (see [`application.md`](./application.md) → Query Rules & Criteria).
  * Current endpoints: `POST /api/v1/accounts` (201, `Location`), `PATCH /api/v1/accounts/{accountId}` (204), `GET /api/v1/accounts` (array of `AccountResponse`), `GET /api/v1/accounts/{accountId}`, `POST /api/v1/categories` (201, no `Location`: there is no `GET /api/v1/categories/{id}`), `GET /api/v1/categories` (array of `CategoryResponse` `{id, name, type, status}`; `type` and `status` are enum names).
  * Operations endpoints (`entry.web.operation`): `POST /api/v1/operations/incomes`, `/expenses`, and `/transfers` (201, `Location`, `{id}`); `POST /api/v1/operations/{operationId}/cancel` (204, no body; the reversal id is not returned); `GET /api/v1/operations/{operationId}` (`OperationResponse`); `GET /api/v1/operations` with optional query parameters `accountId`, `categoryId`, `type`, `from`, `to`, `page`, `pageSize`, answering `{items, page, pageSize}` (`OperationPageResponse`).
    * `page` and `pageSize` are the applied values; the defaults come from `OperationSearchCriteria.forUser` (1 and 20) and `pageSize` must be 1..100. Order is newest first (`operationDate` DESC, `id` DESC). There is no total: the last page is the one with fewer than `pageSize` items, and a page past the end returns `[]` in `items`.
    * `OperationResponse` is `{id, type, amountCents, operationDate, status, cancelledAt, account, category, transfer: {sourceAccount, targetAccount}}`. Transfers have `status`, `account`, and `category` null (and `cancelledAt` null); incomes and expenses have `transfer` null. `cancelledAt` is an ISO-8601 UTC instant.
    * Query enum parameters are case-sensitive (`type=income` answers 400; use `INCOME`). Cancelling a transfer answers 404, because the Core does not treat transfers as cancellable.
    * Registrations are not idempotent: a retry after a timeout creates a duplicate. An `Idempotency-Key` is deferred.
  * Obligations endpoints (`entry.web.obligation`, controllers `ObligationController` and `ObligationOccurrenceController`):
    * `POST /api/v1/obligations` with `{name, amountCents, accountId, categoryId, recurrence: {frequency, startDate, endDate?}}` answers 201 `{id}` with `Location` `/api/v1/obligations/{id}`.
    * `GET /api/v1/obligations` answers a bare JSON array of `ObligationResponse` (unpaginated, all statuses, ordered by name then id; `[]` when none) and `GET /api/v1/obligations/{obligationId}` answers one `ObligationResponse` `{id, name, amountCents, account: {id, name}, category: {id, name}, recurrence: {frequency, startDate, endDate}, status}` (`endDate` is `null` when the calendar has no end; `status` is `ACTIVE` or `ARCHIVED`); a missing obligation or another user's answers 404.
    * `PATCH /api/v1/obligations/{obligationId}` is partial: a null or absent field keeps its value and a field cannot be cleared. A `recurrence` replaces the whole calendar, and a null `endDate` inside it removes the end. `{}` answers 400 "At least one change is required"; no body answers 400. Answers 204.
    * `POST /api/v1/obligations/{obligationId}/archive` answers 204.
    * `POST /api/v1/obligations/{obligationId}/occurrences/{dueDate}/pay` takes an optional body `{amountCents?, operationDate?, accountId?, categoryId?}` and answers 201 with `Location` `/api/v1/operations/{operationId}` and body `{operationId}` (not `{id}`). To undo a payment, use `POST /api/v1/operations/{operationId}/cancel`.
    * `.../occurrences/{dueDate}/skip` answers 204; `.../occurrences/{dueDate}/reopen` answers 204 (a paid occurrence answers 409 `OCCURRENCE_PAID_NOT_REOPENABLE`).
    * `POST /api/v1/obligations/{obligationId}/overdue-occurrences/skip` answers 200 `{skippedDueDates: [...]}` (`[]` when none).
    * Required-field errors for nested objects use dotted names (`Field 'recurrence.frequency' is required`).
    * Payments are not idempotent, except for the natural `OCCURRENCE_ALREADY_RESOLVED` guard on the same occurrence.
  * Dashboard endpoint (`entry.web.dashboard`, `DashboardController`): `GET /api/v1/dashboard` answers 200 `{horizon: {from, to}, availableToSpend: {balanceCents, committedCents, availableCents, shortfallCents}, attention: [...], upcomingCommitments: [{obligation, dueDate, amountCents, account, paymentBlocked}], recent: [{id, type, amountCents, operationDate, account, category, transfer, obligation}]}`.
    * `attention` keeps the Core's priority order; each item has a `type` and its own fields: `OVERDUE_OCCURRENCE` `{obligation, oldestOverdue, overdueCount, overdueAmountCents}`, `SHORTFALL` `{shortfallCents}`, `PAYMENT_BLOCKED` `{obligation, account, nearestDueDate, committedCents, accountInactive, categoryInactive}`, `ACCOUNT_SHORTFALL` `{account, shortfallCents, nearestDueDate}`.
    * Limits: horizon of 14 days, 5 upcoming commitments, 5 recent operations. A user without data gets zero amounts and empty lists (`[]`).
    * The read-transaction note under Transaction Management (`busy_timeout`) now applies to a live endpoint.
* **Product queries**: the Dashboard is implemented (`GetDashboard`, `DashboardQueryPort`, `GET /api/v1/dashboard`; single-user mode, no authentication needed); Review is still planned. See [`docs/dashboard-review-read-models.md`](../../docs/dashboard-review-read-models.md).

---

## Persistence & Transaction Architecture

### Technical Stack
* **Database**: SQLite
* **Access Strategy**: Plain JDBC (`PreparedStatement`, `ResultSet`, try-with-resources)
* **ORM/JPA**: **None**. No Hibernate, JPA, or ORM frameworks.
* **Connection Pool**: **None**. Connections are managed per transaction or query lifecycle.

### Transaction Management (`JdbcTransactionManager`)
* Single JDBC `Connection` per transaction bound to a `ThreadLocal` (`TransactionConnectionHolder`).
* Command repositories obtain the active connection via `TransactionConnectionHolder.get()`. Calling command repository methods outside an active transaction fails fast (`IllegalStateException`).
* Nested transactions are explicitly rejected.
* Every connection sets `PRAGMA busy_timeout = 5000` (`SQLiteConnectionProvider`), so a writer waits up to 5 s for a reader's SHARED lock before failing with `SQLITE_BUSY`.
* Remaining risk: two concurrent read-then-write transactions make SQLite fail one of them immediately with `SQLITE_BUSY` (deadlock avoidance, the timeout does not apply), which answers 500. A possible future fix is `BEGIN IMMEDIATE` (verify the behavior first).
* No WAL mode: it is a file-level setting and would break single-file backups.
* `GetDashboard` runs its read-only queries in one transaction, which holds a SQLite SHARED lock from the first `SELECT` until commit; a concurrent writer's commit waits for it within the busy timeout.
* Transaction lifecycle: `setAutoCommit(false)` -> execute work -> `commit()` -> clear `ThreadLocal` -> `close()`. On exception: `rollback()` -> clear `ThreadLocal` -> `close()`.

### Query Adapter Dual-Mode Connection Strategy
* Query adapters (`JdbcAccountQueryAdapter`, `JdbcCategoryQueryAdapter`, `JdbcObligationQueryAdapter`, `JdbcFinancialOperationQueryAdapter`, `JdbcDashboardQueryAdapter`, `JdbcUserQueryAdapter`) support both transaction-bound and independent execution:
  * **Inside active transaction**: Reuses `TransactionConnectionHolder.get()` without closing the bound connection.
  * **Outside active transaction**: Opens a short-lived connection via `SQLiteConnectionProvider.getConnection()` and closes it via try-with-resources.

### Polymorphic Reversal Schema Limitation
* Table `reversals.original_operation_id` has no database foreign key constraint because the original operation may belong to `income_operations` or `expense_operations`.
* Referential integrity and polymorphic lookup are enforced by application logic (`CancelOperation`) and query adapter joins (`UNION ALL` across operation tables in `JdbcReversalRepository` and `JdbcFinancialOperationQueryAdapter`).

---

## Dependency Rule

Dependencies always point inward:

```text
Entry
   ↓
Infrastructure
      ↓
Application
      ↓
Domain
```

The Entry layer is outermost: it may depend on all inner layers, and no inner layer depends on it. The Domain never depends on Application or Infrastructure. Application never depends on concrete Infrastructure classes. Infrastructure depends inward on Application contracts and Domain aggregates.

