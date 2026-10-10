# Application Guidelines

## Core Application Status

The Core Application layer implementation is **complete and validated** as of milestone `milestone/core-mvp-complete`.

The Application layer coordinates application use cases. It depends on the Domain layer but must not depend on Infrastructure implementations.

```text
Application
    ↓
Domain
```

Infrastructure implements the contracts (ports) defined by the Application layer.

---

## Use Cases

Each business capability is represented by a dedicated use case class in `com.rauldev.personalfinance.application.usecase`:

### Command Use Cases (Write Side)
* `CreateAccount`: Creates a new account for a user (`balance = 0`, `ACTIVE`).
* `ModifyAccount`: Renames an account (`ModifyAccountCommand` carries only `userId`, `accountId`, and `name`). No use case activates or deactivates accounts or categories.
* `CreateCategory`: Creates a new category (`INCOME` or `EXPENSE`, `ACTIVE`).
* `RegisterIncome`: Registers an income operation and credits the account balance.
* `RegisterExpense`: Registers an expense operation and debits the account balance.
* `RegisterTransfer`: Registers an atomic transfer between two accounts belonging to the same user.
* `CancelOperation`: Cancels an `Income` or `Expense` operation, updates account balance, and records a `Reversal`. When it cancels an `Expense` that paid an obligation occurrence, it also deletes that occurrence's resolution in the same transaction (see below).
* `CreateObligation`: Creates an `ACTIVE` obligation after checking that the account and category belong to the user and that the name is not taken.
* `ModifyObligation`: Partial update of an obligation (name, amount, payment source, recurrence) in a single transaction.
* `ArchiveObligation`: Archives an obligation (requires no overdue occurrences).
* `SkipOccurrence`: Skips one scheduled occurrence (future dates of the calendar included) by saving a `SKIPPED` resolution. Returns the obligation id.
* `SkipOverdueOccurrences`: Skips every overdue occurrence of an obligation in a single transaction. Returns the skipped due dates in ascending order; with nothing overdue it is a no-op and returns an empty list (an archived obligation still fails).
* `ReopenOccurrence`: Reopens a `SKIPPED` occurrence by deleting its resolution; a `PAID` one is reopened by cancelling its expense. Returns the obligation id. It does not check the date against the current calendar.
* `PayOccurrence`: Pays one occurrence in a single transaction: registers an expense through `ExpenseRegistration` and saves a `PAID` resolution with its id. Returns the **expense id** (the client uses it to undo the payment by cancelling the expense). Defaults to the obligation's account, category, and amount and to today; `PayOccurrenceCommand` accepts optional `amount` (> 0, the real amount is recorded), `operationDate` (cannot be after today), `accountId`, and `categoryId` (alternatives, for example to unblock a payment). Future dates of the calendar can be paid (advance payment). Check order: operation date, obligation (404), `OccurrenceResolution.validateNewResolution` (`OBLIGATION_ARCHIVED`, `OCCURRENCE_NOT_SCHEDULED`), existing resolution (`OCCURRENCE_ALREADY_RESOLVED`), then the expense registration.

All obligation command use cases except `ReopenOccurrence` receive a `java.time.Clock` as the last constructor parameter and compute today inside the transaction. `CancelOperation` also receives a `Clock` (last constructor parameter) and uses `Instant.now(clock)` for the reversal timestamp. `GetDashboard(DashboardQueryPort, TransactionManager, Clock)` also takes a `Clock` (last) and computes today inside the transaction. The obligations design and step history are in [`docs/obligations-core-proposal.md`](../../docs/obligations-core-proposal.md); step 3 (use cases) is complete, the obligation endpoints exist (E4), and the dashboard read side (step 4) is implemented and exposed by `GET /api/v1/dashboard` (E5, see [`architecture.md`](./architecture.md) → Entry Layer).

### Shared Application Component
* `ExpenseRegistration` (`ExpenseRegistration(AccountRepository, CategoryRepository, ExpenseOperationRepository)`, `Expense register(RegisterExpenseCommand)`): loads the account and then the category by user (`ResourceNotFoundException`), calls `Expense.register`, debits the account, and saves the expense and then the account. It is **not** a use case and opens **no transaction**: callers run it inside their own `TransactionManager.execute`, because nested transactions are rejected. `RegisterExpense` keeps its public constructor, builds an `ExpenseRegistration` internally, and executes `expenseRegistration.register(command).id()` in its transaction; `PayOccurrence` receives it by constructor. The entry layer exposes it as a bean (`ObligationUseCaseConfiguration`) only for `PayOccurrence`; `RegisterExpense` is wired without it.

### `CancelOperation` and resolutions
When cancelling an `Expense`, after the reversal is saved and the balances updated, and in the same transaction, `CancelOperation` runs `occurrenceResolutionRepository.findByExpenseId(expense.id()).ifPresent(r -> delete(r.id()))`; the occurrence returns to pending or overdue. The expense is loaded with `findByIdAndUserId` before the (unscoped) resolution lookup, so ownership is checked first. Incomes never touch resolutions. If the cancellation fails, the resolution stays.

### Query Use Cases (Read Side)
* `GetAccount`: Retrieves account details (`AccountDetails`) by account ID and user ID.
* `ListAccounts`: Lists all the user's accounts (`List<AccountDetails>`), active and inactive, with a single `AccountQueryPort.findByUserId` call and no transaction.
* `ListCategories`: Lists all the user's categories (`List<CategoryDetails>`), active and inactive, with a single `CategoryQueryPort.findByUserId` call and no transaction.
* `ListObligations`: Lists all the user's obligations (`List<ObligationDetails>`), active and archived, with a single `ObligationQueryPort.findByUserId` call and no transaction.
* `GetObligation`: Retrieves an obligation (`ObligationDetails`) by obligation ID and user ID with a single `ObligationQueryPort.findByIdAndUserId` call and no transaction; a missing obligation or another user's throws `ResourceNotFoundException` ("Obligation not found for user: <id>").
* `GetOperationHistory`: Retrieves a paginated history (`List<FinancialOperationHistoryItem>`) using search criteria.
* `GetOperationDetails`: Retrieves detailed operation information (`FinancialOperationDetails`) by operation ID and user ID.
* `GetDashboard`: Builds the user's `Dashboard` with `CommitmentCalculator`. Unlike the other query use cases, it runs all five port reads inside one `transactionManager.execute` (consistent snapshot, one connection). Limits (private constants): horizon 14 days, upcoming commitments 5, recent activity 5.

Use cases maintain single responsibility and operate on application-level DTOs/Commands/Queries.

---

## Output Ports

Output ports in `com.rauldev.personalfinance.application.port.out` define infrastructure contracts:

### Command Repositories
* `AccountRepository`: Persistence contract for `Account` aggregate.
* `CategoryRepository`: Persistence contract for `Category` aggregate.
* `IncomeOperationRepository`: Persistence contract for `Income` aggregate.
* `ExpenseOperationRepository`: Persistence contract for `Expense` aggregate.
* `TransferOperationRepository`: Persistence contract for `Transfer` aggregate.
* `ReversalRepository`: Persistence contract for `Reversal` aggregate.
* `UserRepository`: Persistence contract for the `User` reference. Defines `User create(User user)` and `void deleteById(UUID userId)`; deleting a missing id is a no-op.

* `ObligationRepository`: Persistence contract for the `Obligation` aggregate, user-scoped. Defines `create`, `findByIdAndUserId(UUID id, UUID userId)`, `existsByUserIdAndName(UUID userId, String name)`, `existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID obligationId)` and `update`. Unlike the default method of `AccountRepository`, `existsByUserIdAndNameAndIdNot` is backed by SQL in the adapter.
* `OccurrenceResolutionRepository`: Persistence contract for `OccurrenceResolution`. Defines `create`, `findByObligationId(UUID)` (ordered by due date), `findByObligationIdAndDueDate(UUID, LocalDate)`, `findByExpenseId(UUID)` and `void delete(UUID resolutionId)` (deleting a missing id is a no-op). It is **not** user-scoped: callers load the obligation with `ObligationRepository.findByIdAndUserId` first, which checks ownership.

### Query Ports (CQRS Read Side)
* `AccountQueryPort`: Defines `Optional<AccountDetails> findByIdAndUserId(UUID accountId, UUID userId)` and `List<AccountDetails> findByUserId(UUID userId)` (all the user's accounts, ACTIVE and INACTIVE; empty list when none).
* `CategoryQueryPort`: Defines `List<CategoryDetails> findByUserId(UUID userId)` (all the user's categories, ACTIVE and INACTIVE; empty list when none).
* `ObligationQueryPort`: Defines `List<ObligationDetails> findByUserId(UUID userId)` (all the user's obligations, ACTIVE and ARCHIVED, ordered by name then id; empty list when none) and `Optional<ObligationDetails> findByIdAndUserId(UUID obligationId, UUID userId)`.
* `FinancialOperationQueryPort`: Defines `List<FinancialOperationHistoryItem> search(OperationSearchCriteria criteria)` and `Optional<FinancialOperationDetails> findDetailByIdAndUserId(UUID operationId, UUID userId)`.
* `DashboardQueryPort`: Defines `findActiveObligations(UUID userId)`, `findResolutionsOfActiveObligations(UUID userId)` (PAID and SKIPPED resolutions of the user's ACTIVE obligations, in one query), `findAccounts(UUID userId)`, `findCategories(UUID userId)` (both include ACTIVE and INACTIVE) and `findRecentActivity(UUID userId, int limit)`; the first four return domain snapshots and the last returns `List<RecentActivityItem>`. All are user-scoped; `findRecentActivity` with `limit < 1` throws `IllegalArgumentException` ("Limit must be greater than zero").

* `UserQueryPort`: Defines `boolean existsById(UUID userId)`. It is used by the entry layer (`ConfiguredSingleUserProvider`), not by any use case.

### Transaction Management
* `TransactionManager`: Defines `<T> T execute(Supplier<T> transactionalWork)` and default `execute(Runnable)`.

---

## Query Side & Read Models

Query use cases return dedicated CQRS read model projections (`com.rauldev.personalfinance.application.readmodel`), bypassing domain aggregate reconstruction:

* `AccountDetails`: `(UUID id, UUID userId, String name, Money balance, AccountStatus status)`
* `CategoryDetails`: `(UUID id, String name, CategoryType type, CategoryStatus status)`; all required ("Category id cannot be null", "Category name cannot be null", "Category type cannot be null", "Category status cannot be null") and the name cannot be blank ("Category name cannot be empty").
* `AccountSummary`: `(UUID id, String name)`
* `CategorySummary`: `(UUID id, String name)`
* `TransferDetails`: `(AccountSummary sourceAccount, AccountSummary targetAccount)`
* `FinancialOperationHistoryItem`: `(UUID operationId, OperationType operationType, Money amount, LocalDate operationDate, OperationStatus status, Instant cancelledAt, AccountSummary account, CategorySummary category, TransferDetails transfer)`
* `FinancialOperationDetails`: `(UUID operationId, OperationType operationType, Money amount, LocalDate operationDate, OperationStatus status, Instant cancelledAt, AccountSummary account, CategorySummary category, TransferDetails transfer)`
* `ObligationSummary`: `(UUID id, String name)`
* `ObligationDetails`: `(UUID id, String name, Money amount, AccountSummary account, CategorySummary category, Recurrence recurrence, ObligationStatus status)`; `recurrence` is the immutable domain value object (like `Money`), whose `endDate()` is empty when the calendar has no end. All required ("Obligation id cannot be null", "Obligation name cannot be null", "Obligation amount cannot be null", "Obligation account cannot be null", "Obligation category cannot be null", "Obligation recurrence cannot be null", "Obligation status cannot be null") and the name cannot be blank ("Obligation name cannot be empty").
* `DashboardHorizon`: `(LocalDate from, LocalDate to)`, both inclusive; `from` after `to` is rejected ("Horizon start cannot be after its end").
* `AvailableToSpend`: `(Money balance, Money committed, Money available, Money shortfall)`; `available` and `shortfall` cannot both be positive ("Available and shortfall cannot both be positive") and `balance + shortfall` must equal `committed + available` ("Available and shortfall are inconsistent with balance and committed").
* `AttentionItem` (sealed, `AttentionType type()`): `OverdueOccurrence(ObligationSummary obligation, LocalDate oldestOverdue, long overdueCount, Money overdueAmount)` (`overdueCount` > 0, "Overdue count must be greater than zero"), `Shortfall(Money shortfall)`, `PaymentBlocked(ObligationSummary obligation, AccountSummary account, LocalDate nearestDueDate, Money committed, boolean accountInactive, boolean categoryInactive)` (at least one flag true, "A blocked payment requires an inactive account or category") and `AccountShortfall(AccountSummary account, Money shortfall, LocalDate nearestDueDate)`.
* `UpcomingCommitment`: `(ObligationSummary obligation, LocalDate dueDate, Money amount, AccountSummary account, boolean paymentBlocked)`
* `RecentActivityItem`: `(UUID operationId, OperationType operationType, Money amount, LocalDate operationDate, AccountSummary account, CategorySummary category, TransferDetails transfer, ObligationSummary obligation)`; same account/category/transfer rules as the history items, and `obligation` (nullable, the obligation whose occurrence an expense paid) only on `EXPENSE` ("Only expense operations can reference an obligation").
* `Dashboard`: `(DashboardHorizon horizon, AvailableToSpend availableToSpend, List<AttentionItem> attention, List<UpcomingCommitment> upcomingCommitments, List<RecentActivityItem> recent)`; lists are copied defensively.

### Query Rules & Criteria
* `OperationSearchCriteria`: Encapsulates mandatory `userId`, `page` (>= 1), `pageSize` (1..100), and optional filters (`accountId`, `categoryId`, `operationType`, `from`, `to`).
* `OperationType`: Contains `INCOME`, `EXPENSE`, `TRANSFER`. `REVERSAL` is **not** an `OperationType`; reversals surface as `status = CANCELLED` and non-null `cancelledAt` on Income or Expense items.
* Ordering: Operation history results are strictly ordered by `operation_date DESC, id DESC`. Dashboard recent activity (`findRecentActivity`) is ordered by `operation_date DESC, created_at DESC, id DESC` and contains `ACTIVE` operations only.
* Ordering of lists: `AccountQueryPort.findByUserId`, `CategoryQueryPort.findByUserId`, and `ObligationQueryPort.findByUserId` return all statuses ordered by `name, id`, using SQLite's default `BINARY` collation (case-sensitive: uppercase sorts before lowercase and accented letters after `Z`). Clients that show these lists to users should sort with the user's locale.
* User Scoping: All query ports enforce user isolation via mandatory `userId`.

---

## Transaction Boundaries & Aggregate Coordination

* Command use cases requiring multi-repository atomicity wrap execution in `TransactionManager.execute(...)`.
* Example (`RegisterTransfer`):
  ```text
  TransactionManager.execute
  ├── debit source account
  ├── credit target account
  ├── insert transfer
  ├── update source account
  └── update target account
  ```
* Cross-aggregate rules (e.g. verifying `account.userId == category.userId`) are coordinated by the use case before performing aggregate state changes.

---

## Application DTOs & Commands

* Command Objects: `CreateAccountCommand`, `ModifyAccountCommand`, `CreateCategoryCommand`, `RegisterIncomeCommand`, `RegisterExpenseCommand`, `RegisterTransferCommand`, `CancelOperationCommand`, `CreateObligationCommand`, `ModifyObligationCommand`, `ArchiveObligationCommand`, `PayOccurrenceCommand`, `SkipOccurrenceCommand`, `SkipOverdueOccurrencesCommand`, `ReopenOccurrenceCommand`.
* Query Objects: `GetAccountQuery`, `ListAccountsQuery`, `ListCategoriesQuery`, `ListObligationsQuery`, `GetObligationQuery`, `GetOperationDetailsQuery`, `GetDashboardQuery`, `OperationSearchCriteria`.

---

## Error Handling

Application use cases expose meaningful, application-level exceptions:

* `ResourceNotFoundException`: Thrown when a requested account, category, operation, or obligation does not exist or does not belong to the user (including the account and category loaded by `ExpenseRegistration`, so by `RegisterExpense` and `PayOccurrence`, and the obligation of `PayOccurrence`), and by `ReopenOccurrence` when the occurrence has no resolution.
* `IllegalArgumentException`: Invalid input.
* `BusinessRuleViolationException` (`domain`): A business rule violated against stored state. Besides the domain (including "already cancelled", which `CancelOperation` gets by calling `ensureCancellable()`), it is thrown by `CreateAccount`, `ModifyAccount`, `CreateCategory`, `CreateObligation`, `ModifyObligation` (duplicate names), `SkipOccurrence` and `PayOccurrence` (`OCCURRENCE_ALREADY_RESOLVED` pre-check), and by `JdbcAccountRepository` / `JdbcCategoryRepository` / `JdbcObligationRepository` for concurrent duplicate names and by `JdbcOccurrenceResolutionRepository` for a concurrent resolution of the same occurrence. See `domain.md` → Business Rule Violations.
* `IllegalStateException`: Technical faults only (command repository used outside a transaction, nested transaction). The entry layer's `SingleUserNotProvisionedException` (a subclass) is a configuration fault: the configured single user does not exist in the database; it maps to 500 with the message hidden. `GetDashboard` also throws it ("Dashboard data is inconsistent: ...", original `IllegalArgumentException` kept as cause) when `CommitmentCalculator` rejects a missing account or category reference, which can only come from corrupt or cross-user data; it maps to 500.
* Infrastructure exceptions (e.g. `SQLException`) are caught and wrapped inside Infrastructure adapters; they never leak into the Application layer. The exception is a `UNIQUE(user_id, name)` violation on accounts, categories, or obligations, and a `UNIQUE(obligation_id, due_date)` violation on occurrence resolutions, which the adapters translate into `BusinessRuleViolationException` (`*_NAME_ALREADY_EXISTS`, `OCCURRENCE_ALREADY_RESOLVED`) keeping the `SQLException` as cause. Other constraint violations (`UNIQUE(expense_id)`, `CHECK`, foreign keys) are not translated. The messages are shared through `ApplicationConstants`.
* `CorruptedPersistedDataException` (`infrastructure.persistence`): Thrown by adapters when a stored row cannot be mapped to a domain object or read model. It is a server fault, never a client error, and does not extend `IllegalArgumentException`.

### HTTP Mapping (Entry layer, `ApiExceptionHandler`)

Errors are answered as RFC 9457 problem details:

| Exception | Status | Title | Detail |
|---|---|---|---|
| `ResourceNotFoundException` | 404 | `Resource not found` | exception message |
| `IllegalArgumentException` | 400 | `Invalid request` | exception message |
| `BusinessRuleViolationException` | 409 | `Business rule violation` | exception message, plus ProblemDetail property `code` (`BusinessRuleCode.name()`) |
| Spring MVC exceptions (malformed body, unknown route, ...) | standard 4xx | standard | standard, via `ResponseEntityExceptionHandler` |
| Anything else, including `CorruptedPersistedDataException` and `IllegalStateException` (technical faults) | 500 | `Internal server error` | fixed `Unexpected error`; logged server-side |

---

# User Context

Use cases operate in the context of a user.

When accessing user-owned resources, the user identity must be part of the application request or execution context.

Do not trust entity IDs alone when authorization depends on ownership.

Example:

```text
findByIdAndUserId(accountId, userId)
```

is preferred over loading an account only by ID when the operation is user-scoped.

---

# Dependency Injection

Application components should receive their dependencies rather than instantiate them internally.

Prefer constructor injection:

```java
public CreateAccountUseCase(
    AccountRepository accountRepository
) {
    this.accountRepository = accountRepository;
}
```

Do not instantiate repositories or infrastructure services inside use cases.

---

# General Rules

* Keep use cases focused.
* Keep orchestration in Application.
* Keep business invariants in Domain.
* Keep persistence implementation in Infrastructure.
* Depend on interfaces rather than infrastructure implementations.
* Do not expose database models from Application.
* Do not execute SQL from Application.
* Do not introduce abstractions without a concrete need.
* Prefer explicit use-case dependencies.
* Keep transaction boundaries at the use-case level.
