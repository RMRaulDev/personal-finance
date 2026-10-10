# Domain Guidelines

## Core Domain Status

The Core Domain implementation is **complete and validated** as of milestone `milestone/core-mvp-complete`.

The domain is being extended with **Obligations** (step 1 of Obligations Core, domain only, beyond that milestone). The design is in [`docs/obligations-core-proposal.md`](../../docs/obligations-core-proposal.md); persistence (step 2) is implemented; the lifecycle use cases (step 3a: `CreateObligation`, `ModifyObligation`, `ArchiveObligation`) exist; the skip use cases (step 3b: `SkipOccurrence`, `SkipOverdueOccurrences`, `ReopenOccurrence`) exist; the pay use case (step 3c: `PayOccurrence`) exists, so step 3 is complete; the dashboard read side (step 4: `GetDashboard`, `DashboardQueryPort`) is implemented and exposed by `GET /api/v1/dashboard` (E5, single-user mode through `ConfiguredSingleUserProvider`).

The project uses **Domain-Driven Design (DDD)**. The Domain layer represents the business concepts and rules of the personal finance application, operating completely independent of persistence, frameworks, or delivery mechanisms.

The domain model contains:

```text
Aggregate Roots:
- Account
- Category
- FinancialOperation (Income, Expense, Transfer)
- Reversal
- Obligation

Entities:
- User
- OccurrenceResolution (part of the Obligation model; identity equality by id)

Value Objects:
- Money
- Recurrence

Input snapshots (read-only records for CommitmentCalculator):
- ObligationSnapshot, ResolutionSnapshot, AccountSnapshot, CategorySnapshot

Result records:
- CommitmentSummary, ObligationCommitment, AccountCommitment
- Attention (sealed: OverdueOccurrence, Shortfall, PaymentBlocked, AccountShortfall)

Enums:
- Frequency, ObligationStatus, ResolutionStatus, AttentionType

Domain Service:
- CommitmentCalculator

Package-private helper:
- OperationReferences (shared registration checks for Income, Expense, and Obligation)

Domain Exceptions:
- BusinessRuleViolationException
- BusinessRuleCode
```

---

## User

`User` is an identity entity representing a system user (`com.rauldev.personalfinance.domain.User`).

* Identifiers: `id: UUID`.
* It is not currently modeled as an Aggregate Root.
* Other domain objects reference the user through `userId: UUID`.

---

## Account

`Account` is an Aggregate Root (`com.rauldev.personalfinance.domain.Account`).

Structure:

```text
Account
├── id: UUID
├── userId: UUID
├── name: String
├── balance: Money
└── status: AccountStatus
```

### Account Invariants & Rules

* `id` and `userId` are required (`UUID`).
* `name` is required and cannot be blank.
* `balance` cannot be negative.
* `status` is a valid `AccountStatus` (`ACTIVE`, `INACTIVE`).
* Account names are unique per user (`userId + name`).

### Initial State

A newly created account starts with:

```text
balance = Money.ofCents(0)
status = AccountStatus.ACTIVE
```

Account creation does not accept an initial balance.

### Account Behavior

Exposes state mutation methods enforcing invariants:

```text
activate()
deactivate()
rename(name)
credit(amount)
debit(amount)
```

* `credit(amount)`: requires a strictly positive `Money` amount (> 0).
* `debit(amount)`: requires a strictly positive `Money` amount (> 0) and enforces `balance >= debitAmount`. Rejects debit attempts that would cause a negative balance by throwing `BusinessRuleViolationException` with code `INSUFFICIENT_BALANCE`.

### Account Status

Accounts can be `ACTIVE` or `INACTIVE`. Deactivation does not delete the account; historical operations may continue to reference an inactive account (they still load). An inactive account cannot be used to register a new `Income`, `Expense`, or `Transfer`.

---

## Category

`Category` is an Aggregate Root (`com.rauldev.personalfinance.domain.Category`).

Structure:

```text
Category
├── id: UUID
├── userId: UUID
├── name: String
├── type: CategoryType
└── status: CategoryStatus
```

### Category Invariants & Rules

* `id` and `userId` are required (`UUID`).
* `name` is required and cannot be blank.
* `type` is required (`CategoryType.INCOME` or `CategoryType.EXPENSE`) and is **immutable** after creation.
* `status` is a valid `CategoryStatus` (`ACTIVE`, `INACTIVE`).
* Category names are unique per user (`userId + name`), enforced strictly regardless of `CategoryType`.
* Categories are not physically deleted; deactivation (`deactivate()`) is used instead.

### Category Type & Usage

* `INCOME` categories classify `Income` operations.
* `EXPENSE` categories classify `Expense` operations.
* Cross-classification (e.g., using an `INCOME` category for an `Expense`) is strictly rejected by domain validation.
* An `INACTIVE` category cannot be used to register a new `Income` or `Expense`. Historical operations that reference a category deactivated later still load.

---

## FinancialOperation

`FinancialOperation` is an abstract base class (`com.rauldev.personalfinance.domain.FinancialOperation`) for financial events:

```text
FinancialOperation
├── Income
├── Expense
└── Transfer
```

### Common Properties

```text
FinancialOperation
├── id: UUID
├── userId: UUID
├── amount: Money
└── operationDate: LocalDate
```

### Common Invariants

* `id`, `userId`, and `operationDate` (`LocalDate`) are required.
* `amount` (`Money`) must be strictly positive (`amount > 0`), enforced via `requirePositive(amount)`.

---

## Income

`Income` represents money entering an account (`com.rauldev.personalfinance.domain.Income`).

Structure:

```text
Income
├── accountId: UUID
├── categoryId: UUID
└── status: OperationStatus
```

### Income Invariants & Rules

* `accountId` and `categoryId` are required.
* `category.type` must be `CategoryType.INCOME`.
* `account.userId` and `category.userId` must match `userId`.
* `account.status` must be `ACTIVE` when registering an income (`BusinessRuleViolationException`, `ACCOUNT_INACTIVE`, "Account must be active").
* `category.status` must be `ACTIVE` when registering an income (`BusinessRuleViolationException`, `CATEGORY_INACTIVE`, "Category must be active").
* Registration check order: required references, same user, category type (`IllegalArgumentException`), account active, category active. The shared checks live in the package-private `OperationReferences.validate`, also used by `Expense` and `Obligation`.
* These checks apply to `Income.register` only. Loading a persisted income uses the constructor, so historical operations load even if their account or category was deactivated later.
* `status` is `OperationStatus.ACTIVE` or `OperationStatus.CANCELLED`.
* `Income` is cancellable via `cancel()`. `ensureCancellable()` throws `BusinessRuleViolationException` with `OPERATION_ALREADY_CANCELLED` if the income is already cancelled; `cancel()` calls it first.

---

## Expense

`Expense` represents money leaving an account (`com.rauldev.personalfinance.domain.Expense`).

Structure:

```text
Expense
├── accountId: UUID
├── categoryId: UUID
└── status: OperationStatus
```

### Expense Invariants & Rules

* `accountId` and `categoryId` are required.
* `category.type` must be `CategoryType.EXPENSE`.
* `account.userId` and `category.userId` must match `userId`.
* `account.status` must be `ACTIVE` when registering an expense (`BusinessRuleViolationException`, `ACCOUNT_INACTIVE`, "Account must be active").
* `category.status` must be `ACTIVE` when registering an expense (`BusinessRuleViolationException`, `CATEGORY_INACTIVE`, "Category must be active").
* `account.balance` must be sufficient (`balance >= amount`) when registering an expense.
* Registration check order: required references, same user, category type (`IllegalArgumentException`), account active, category active, then balance (both ACTIVE checks run before the balance check).
* These checks apply to `Expense.register` only. Loading a persisted expense uses the constructor, so historical operations load even if their account or category was deactivated later.
* `status` is `OperationStatus.ACTIVE` or `OperationStatus.CANCELLED`.
* `Expense` is cancellable via `cancel()`. `ensureCancellable()` throws `BusinessRuleViolationException` with `OPERATION_ALREADY_CANCELLED` if the expense is already cancelled; `cancel()` calls it first.

---

## Transfer

`Transfer` represents a movement of money between two accounts (`com.rauldev.personalfinance.domain.Transfer`).

Structure:

```text
Transfer
├── sourceAccountId: UUID
└── targetAccountId: UUID
```

### Transfer Invariants & Rules

* `sourceAccountId` and `targetAccountId` are required and must be different (`sourceAccountId != targetAccountId`).
* Both accounts must belong to the same user (`sourceAccount.userId == targetAccount.userId == userId`).
* Both accounts must be `ACTIVE` when registering a transfer.
* Source account must have sufficient balance (`sourceAccount.balance >= amount`).
* `Transfer` has **no `status` field** and **no `cancel()` method**. Transfers are immutable financial operations.

---

## Reversal

`Reversal` is an independent Aggregate Root (`com.rauldev.personalfinance.domain.Reversal`) representing an operation cancellation event.

Structure:

```text
Reversal
├── id: UUID
├── userId: UUID
├── originalOperationId: UUID
├── amount: Money
└── cancelledAt: Instant
```

### Reversal Invariants & Rules

* `id`, `userId`, `originalOperationId`, `amount`, and `cancelledAt` (`Instant`) are required.
* Can only be created for an `Income` or `Expense` operation (`originalOperation instanceof Income || originalOperation instanceof Expense`).
* `amount` is positive and equals the original operation amount.
* Reversal effect is derived from original operation type (reversing an Income debits the account; reversing an Expense credits the account).
* A Reversal has no `categoryId`, no `accountId`, and no `status`.
* Reversals are immutable and cannot be reversed.
* Only one reversal may exist per cancellable original operation (`original_operation_id` is unique).

---

## Obligation

`Obligation` is an Aggregate Root (`com.rauldev.personalfinance.domain.Obligation`) for a payment commitment of the user, single or recurring. It only models money going out.

Structure:

```text
Obligation
├── id: UUID
├── userId: UUID
├── name: String
├── amount: Money
├── accountId: UUID
├── categoryId: UUID
├── recurrence: Recurrence
└── status: ObligationStatus
```

Occurrences are computed from the `Recurrence`, never stored. Only `OccurrenceResolution`s are persisted. The aggregate does not own its resolutions: methods that depend on them receive `today` and **all** resolutions of the obligation. No domain class calls `now()`.

### Obligation Invariants & Rules

* `id`, `userId`, `accountId`, `categoryId`, `recurrence`, and `status` are required.
* `name` is required and cannot be blank. `amount` must be strictly positive.
* `status` is `ObligationStatus.ACTIVE` or `ObligationStatus.ARCHIVED`. Archiving is always explicit; an obligation with nothing left to resolve is not archived automatically.
* Obligation names are unique per user (`OBLIGATION_NAME_ALREADY_EXISTS`); defined in the domain, thrown by `CreateObligation` and `ModifyObligation`, and by `JdbcObligationRepository` for concurrent duplicates.
* `Obligation.create(account, category, name, amount, recurrence, today)` starts `ACTIVE`. It validates the payment source with `OperationReferences.validate` (same order and messages as `Expense.register`, without the balance check; category type must be `EXPENSE`) and rejects a start date more than 31 days before `today` (`IllegalArgumentException`). The constructor is for reconstitution and applies neither the payment-source checks nor the 31-day window.

### Overdue

Overdue occurrences = scheduled dates from `recurrence.startDate()` through yesterday, minus the resolved dates that are on the current calendar. A resolution off the calendar does not reduce the count. It is O(R), with R = resolutions of the obligation. Exposed as `overdueCount` and `unresolvedDatesBetween`. `overdueDates(today, resolutions)` returns those overdue dates (unresolved, from `startDate` through yesterday) in ascending order, with the same range as `overdueCount`; `SkipOverdueOccurrences` uses it. `ensureActive()` is public and throws `OBLIGATION_ARCHIVED` if the obligation is archived; the edit methods and the use cases call it. The oldest overdue date is computed by `CommitmentCalculator` through `Recurrence.firstDateBetweenExcluding`.

### Edit Rules

Edit methods: `rename`, `changeAmount`, `changePaymentSource(account, category)`, `changeRecurrence`, `archive`. All except `rename` receive `today` and all resolutions.

* Every edit on an archived obligation fails with `OBLIGATION_ARCHIVED`, including `rename` and `archive`.
* Every edit except `rename` requires zero overdue occurrences (`OBLIGATION_HAS_OVERDUE_OCCURRENCES`, "Obligation has overdue occurrences"). `rename` is allowed with overdue occurrences.
* `changePaymentSource` also requires the account to belong to the obligation user (`IllegalArgumentException`) and applies the payment-source checks of `create` (`ACCOUNT_INACTIVE`, `CATEGORY_INACTIVE`). Callers changing only one of them pass the current other one.
* `changeRecurrence` check order:
  1. not archived, then no current overdue (as above);
  2. if the frequency or start date changed (re-anchor): start date not before `today` (`IllegalArgumentException`, "Start date cannot be before today"), and not after any resolution due on or after `today` (`OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION`, "Start date cannot be after a resolved upcoming occurrence");
  3. if a new end date is present and changed: not before yesterday (`IllegalArgumentException`, "End date cannot be before yesterday"), and not before any resolution due on or after `today` (`OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION`, "End date cannot be before a resolved upcoming occurrence"). Removing the end date skips this step;
  4. if re-anchored: every resolution due on or after `today` must be on the new calendar (`OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION`, "Upcoming resolution is not on the new calendar"), which prevents paying an occurrence twice;
  5. the resulting calendar cannot have unresolved overdue occurrences (`OBLIGATION_HAS_OVERDUE_OCCURRENCES`, "Change would create overdue occurrences"), which also covers removing or extending an end date already in the past.

---

## Recurrence

`Recurrence` is an immutable Value Object (`com.rauldev.personalfinance.domain.Recurrence`) holding `frequency: Frequency` (`ONCE`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `YEARLY`), `startDate` (the anchor), and an optional inclusive `endDate`. `Obligation` has no other date fields.

* `frequency` and `startDate` are required. `endDate`, if present, cannot be before `startDate` (`IllegalArgumentException`).
* Date `n` is always computed from the anchor, never from the previous date, so clamping never accumulates: `ONCE` is only `startDate`; `WEEKLY` is `startDate + 7·n` days; `BIWEEKLY` is `startDate + 14·n` days (both independent of the month); `MONTHLY` is `startDate.plusMonths(n)`; `YEARLY` is `startDate.plusYears(n)`.
* Month-end clamping without carry-over: a day that does not exist in the month becomes the last day of that month (anchor 31 Jan gives 28/29 Feb, then 31 Mar). Anchor 29 Feb gives 28 Feb in non-leap years and 29 Feb in leap years.
* No date is before `startDate` or after `endDate` (inclusive).
* Operations are pure (no clock, no I/O). `countBetween` and `isScheduled` are O(1); `datesBetween` is O(k) with k = returned dates. `unresolvedDatesBetween` is O(k) with k = scheduled dates in the range, because it enumerates them all and then filters out the resolved ones. `countUnresolvedBetween` and `firstDateBetweenExcluding` take a set of resolved dates and are O(R), with R = resolved dates.

---

## OccurrenceResolution

`OccurrenceResolution` is an entity (`com.rauldev.personalfinance.domain.OccurrenceResolution`) recording what happened to one occurrence, with equality by `id`. It is not an Aggregate Root: it belongs to the Obligation model.

Structure:

```text
OccurrenceResolution
├── id: UUID
├── obligationId: UUID
├── dueDate: LocalDate
├── status: ResolutionStatus
├── expenseId: UUID | null
└── resolvedAt: Instant
```

* `status` is `PAID` or `SKIPPED`. `PAID` requires `expenseId`; `SKIPPED` must have none (`IllegalArgumentException`).
* `dueDate` identifies the occurrence and is unique per obligation.
* The factories `paid(obligation, dueDate, expenseId, resolvedAt)` and `skipped(obligation, dueDate, resolvedAt)` validate only when **creating**: the obligation must not be archived (`OBLIGATION_ARCHIVED`, checked through `obligation.ensureActive()`) and the date must be on its current calendar (`OCCURRENCE_NOT_SCHEDULED`). Future dates of the calendar are allowed (advance payment). Both checks live in the public static `validateNewResolution(Obligation, LocalDate)`, which the factories call and which `PayOccurrence` also calls before the expense exists.
* The constructor is for reconstitution and accepts off-calendar dates, so historical resolutions stay valid after the calendar is re-anchored.
* `ensureReopenable()` throws `OCCURRENCE_PAID_NOT_REOPENABLE` ("Paid occurrence can only be reopened by cancelling its expense") for a `PAID` resolution; only `SKIPPED` can be reopened. A paid occurrence is reopened by cancelling its expense: `CancelOperation` deletes the resolution in the same transaction.
* `OCCURRENCE_ALREADY_RESOLVED` is thrown by `SkipOccurrence` and `PayOccurrence`, and by `JdbcOccurrenceResolutionRepository` for concurrent resolutions of the same date.

---

## CommitmentCalculator

`CommitmentCalculator` is a pure domain service (`com.rauldev.personalfinance.domain.CommitmentCalculator`). It has no clock and no I/O.

* Constructor takes `horizonDays` (must be positive); `DEFAULT_HORIZON_DAYS = 14`. The horizon is `[today, today + horizonDays − 1]`, both inclusive.
* `calculate(today, obligations, resolutions, accounts, categories)` takes input snapshot records (`ObligationSnapshot`, `ResolutionSnapshot`, `AccountSnapshot`, `CategorySnapshot`), not aggregates, so read-side callers need no aggregate reconstruction. `ObligationSnapshot` rejects a non-positive amount. A missing account or category of an ACTIVE obligation is an `IllegalArgumentException`.
* It returns `CommitmentSummary` (`committedAmount`, `balance`, `availableToSpend`, `shortfall`, per-obligation `ObligationCommitment`, per-account `AccountCommitment`, and the complete ordered `attention` list).
* `committed(o) = amount × (overdue count + unresolved dates in the horizon)`, for ACTIVE obligations only, including those whose account or category is inactive. `committedAmount` is the sum.
* `balance` is the sum of ACTIVE accounts.
* Money is never negative, so it compares first: if `balance >= committed`, `availableToSpend = balance − committed` and `shortfall = 0`; otherwise `availableToSpend = 0` and `shortfall = committed − balance`. The same applies per account.
* Per account: only ACTIVE accounts are evaluated, and `committed(a)` sums the obligations paid from it whose account and category are both ACTIVE.
* Attention types are declared in `AttentionType` in priority order and the list is sorted by that order:
  1. `OVERDUE_OCCURRENCE`: one per obligation with overdue occurrences; ties by oldest overdue date, then larger overdue amount, then obligation name, then id.
  2. `SHORTFALL`: global `shortfall > 0`; single.
  3. `PAYMENT_BLOCKED`: obligation with a commitment (overdue or in the horizon) whose account or category is inactive; ties by nearest committed date (oldest overdue, else nearest pending), then larger committed amount, then name, then id.
  4. `ACCOUNT_SHORTFALL`: ACTIVE account with `shortfall(a) > 0`; ties by larger shortfall, then nearest committed date of the account, then account name, then id.

---

## Money

`Money` is an immutable Value Object (`com.rauldev.personalfinance.domain.Money`).

* Currency: Fixed to **MXN** for the MVP. Currency selection is not exposed in account or operation creation.
* Precision: Uses `BigDecimal` with a fixed scale of 2 decimal places (`RoundingMode.UNNECESSARY`).
* Invariant: `amount >= 0`. Negative monetary amounts are strictly rejected by factory methods and arithmetic operations.
* Factories: `Money.of(BigDecimal)`, `Money.of(String)`, `Money.ofCents(long)`.
* Arithmetic & Comparison: `add(Money)`, `subtract(Money)` (rejects a negative result), `multiply(long factor)` (`factor >= 0`, `IllegalArgumentException` otherwise), `compareTo(Money)`, `isZero()`, `isPositive()`. Value-based equality (`equals` / `hashCode`).

---

## Business Rule Violations

A request that is well-formed but violates a business rule given the current state is rejected with `BusinessRuleViolationException` (`com.rauldev.personalfinance.domain`), which carries a `BusinessRuleCode` (`code()`) and a message. Both are required (non-null). An optional constructor `(code, message, cause)` keeps a non-null cause (e.g. the `SQLiteException` behind a concurrent duplicate name).

* It extends `RuntimeException` deliberately, not `IllegalStateException`, which is reserved for technical faults. It has no Spring dependency.
* `BusinessRuleCode` constants are stable, client-facing identifiers serialized via `name()`. They must never be renamed.
* Uniqueness conflicts against stored state are business-rule violations, not input validation.
* Invalid input or shape (null, blank, non-positive amount, ...) stays `IllegalArgumentException`.

| Code | Thrown by |
|---|---|
| `INSUFFICIENT_BALANCE` | `Account.debit`, `Expense.register`, `Transfer.register` |
| `ACCOUNT_INACTIVE` | `Income.register`, `Expense.register`, `Transfer.register`, `Obligation.create`, `Obligation.changePaymentSource` |
| `CATEGORY_INACTIVE` | `Income.register`, `Expense.register`, `Obligation.create`, `Obligation.changePaymentSource` |
| `OPERATION_ALREADY_CANCELLED` | `Income` / `Expense` `ensureCancellable()` and `cancel()` (invoked by `CancelOperation`) |
| `ACCOUNT_NAME_ALREADY_EXISTS` | `CreateAccount`, `ModifyAccount`; `JdbcAccountRepository` (concurrent duplicates) |
| `CATEGORY_NAME_ALREADY_EXISTS` | `CreateCategory`; `JdbcCategoryRepository` (concurrent duplicates) |
| `OBLIGATION_NAME_ALREADY_EXISTS` | `CreateObligation`, `ModifyObligation`; `JdbcObligationRepository` (concurrent duplicates) |
| `OBLIGATION_ARCHIVED` | Every `Obligation` edit method (`rename`, `changeAmount`, `changePaymentSource`, `changeRecurrence`, `archive`); `Obligation.ensureActive` (`SkipOverdueOccurrences`, `ReopenOccurrence`); `OccurrenceResolution.validateNewResolution` (via `paid` / `skipped` and `PayOccurrence`) |
| `OBLIGATION_HAS_OVERDUE_OCCURRENCES` | `Obligation.changeAmount`, `changePaymentSource`, `changeRecurrence`, `archive` (current overdue); `changeRecurrence` (resulting calendar would have overdue) |
| `OCCURRENCE_ALREADY_RESOLVED` | `SkipOccurrence`, `PayOccurrence`; `JdbcOccurrenceResolutionRepository` (concurrent resolutions) |
| `OCCURRENCE_PAID_NOT_REOPENABLE` | `OccurrenceResolution.ensureReopenable` (via `ReopenOccurrence`) |
| `OCCURRENCE_NOT_SCHEDULED` | `OccurrenceResolution.validateNewResolution` (via `paid` / `skipped` and `PayOccurrence`) |
| `OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION` | `Obligation.changeRecurrence` |

The HTTP mapping is described in `application.md` → Error Handling.

---

## Aggregate Boundaries & Domain Principles

* Aggregates (`Account`, `Category`, `FinancialOperation`, `Reversal`, `Obligation`) are independent consistency boundaries. `OccurrenceResolution` belongs to the Obligation model: it references its obligation by `obligationId`, and the `Obligation` does not hold its resolutions (they are passed in).
* Aggregates reference each other strictly by identifier (`UUID`).
* Business rules and state transitions belong exclusively in the Domain.
* Domain classes have zero dependencies on SQLite, JDBC, HTTP, or infrastructure frameworks.
