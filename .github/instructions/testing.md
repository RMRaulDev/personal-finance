# Testing Guidelines

## Stack

* **JUnit Jupiter 6** (`junit-jupiter`, version managed by the Spring Boot BOM) with `org.junit.jupiter.api.Assertions`.
* No mocking or assertion libraries (no Mockito, no AssertJ). Do not add a test dependency without a concrete need (see `java.md` → Dependencies).
* `pom.xml` excludes Mockito, Hamcrest, JSONassert, JsonPath, Awaitility, and XMLUnit from `spring-boot-starter-webmvc-test`. AssertJ remains on the classpath only because `ApplicationContextRunner` needs it to compile; it must not be used in tests.
* Run all tests with `mvn test`; a single class with `mvn test -Dtest=RegisterExpenseTest`.

---

## Structure

* Test classes mirror the production package (`src/test/java/com/rauldev/personalfinance/...`).
* One test class per production class, named `<ProductionClass>Test`, declared package-private (`class RegisterExpenseTest`). The only exceptions are `EntryLayerIsolationTest`, which checks that domain, application, and infrastructure do not reference Spring and that `entry.web` does not reference the infrastructure layer, the use case integration tests named `<Feature>IntegrationTest` (see Infrastructure → Use case integration tests), and entry HTTP tests that need their own Spring context because of cached state, e.g. `SingleUserNotProvisionedTest` (`entry.web`: the configured user is missing; it needs its own context because `ConfiguredSingleUserProvider` caches a positive existence check).
* The package-private rule covers test classes. Shared test helpers (not test classes), like `entry.SqliteTestDatabase`, may be public when used from several packages.
* Separate Arrange / Act / Assert with blank lines. One behavior per test method.

---

## Testing Strategy by Layer

### Domain

* Pure unit tests. Build aggregates and value objects directly with their constructors or factory methods.
* Every invariant in `domain.md` has a test that accepts a valid value and a test that rejects an invalid one.
* Every state transition (`credit`, `debit`, `cancel`, `deactivate`, `rename`, ...) is tested, including the rejected transitions.

### Application (use cases)

* Unit tests with **hand-written fakes**. Do not use real infrastructure in use case unit tests; the only exception is the use case integration tests described under Infrastructure.
* Fakes are `private static final class Recording<Port>` nested in the test class, implementing the output port:
  * They return data configured through the constructor (e.g. `new RecordingAccountRepository(Optional.of(account))`).
  * They record call counters (`findCalls`, `createCalls`, `updateCalls`) and captured arguments (`createdExpense`, `updatedAccount`).
  * `RecordingTransactionManager` records `executed` and runs the work inline.
* Verify:
  * The returned value and the state passed to the ports (e.g. the balance of `updatedAccount`).
  * That the work runs inside the transaction manager.
  * On failure paths, the specific exception **and the absence of side effects** (`createCalls == 0`, `updateCalls == 0`, later lookups not executed).

### Infrastructure (JDBC adapters)

* Integration tests against a **real SQLite database** in a `@TempDir`.
* The schema is always loaded from `src/main/resources/db/schema.sql`. Never copy or hand-write the schema in a test.
* Users (and any other prerequisite rows) are seeded with SQL in `@BeforeEach`.
* Command repositories are exercised through `JdbcTransactionManager`.
* Cover:
  * Round-trip of **every persisted field** (create → find → compare).
  * User scoping: looking up another user's resource returns empty.
  * Ordering guarantees (e.g. `operation_date DESC, id DESC`).
  * Database constraints (foreign keys, `UNIQUE`, `CHECK`).
  * Command repositories fail with `IllegalStateException` outside an active transaction.
  * Query adapters work both inside and outside an active transaction.
  * Every row mapper has a corrupt-row test: a raw invalid row is inserted with SQL and the read fails with `CorruptedPersistedDataException`, whose message contains the source and the row id and whose cause is preserved.

#### Use case integration tests

* Named `<Feature>IntegrationTest` (e.g. `ObligationPaymentIntegrationTest`), in package `com.rauldev.personalfinance.infrastructure`.
* They wire use cases to the real JDBC adapters and `JdbcTransactionManager`. Use them only when a rule depends on real transaction rollback or database constraints (e.g. the expense and balance roll back when a resolution race fails, and a failed cancellation keeps the resolution). Use case unit tests still use fakes.
* Same rules as the other infrastructure tests: SQLite in a `@TempDir`, schema loaded from `schema.sql`, prerequisite rows seeded with SQL.
* Method names are descriptive present-tense sentences, as in the other infrastructure tests.

### Entry (Spring)

* `ApplicationContextRunner` for configuration and fail-fast startup behavior (e.g. missing SQLite URL).
* `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `java.net.http.HttpClient` for HTTP error mapping and controllers. Do not use MockMvc: it rethrows unmapped exceptions instead of answering the 500 problem detail.
* SQLite URLs point to a `@TempDir` and are supplied with `@DynamicPropertySource`.

#### Controller tests

* One test class per controller, named `<Controller>Test` (e.g. `AccountControllerTest`, `CategoryControllerTest`), in the controller's package.
* They run `@SpringBootTest(webEnvironment = RANDOM_PORT)` and call the server with `java.net.http.HttpClient`, against a real SQLite file in a `static @TempDir`. `@DynamicPropertySource` sets `personal-finance.sqlite.url` and `personal-finance.single-user-id`.
* The database is the shared helper `com.rauldev.personalfinance.entry.SqliteTestDatabase`: it loads the schema from `schema.sql`, `reset()` deletes all rows in foreign-key order, and it has insert helpers (`insertUser`, `insertAccount`, `insertCategory`, `insertIncome`, `insertExpense`, `insertTransfer`, `insertObligation`, `insertResolution`) plus `count` and `queryFirstColumn` to assert persisted state. `insertResolution` sets a fixed `resolved_at`, and `insertObligation` writes the row directly, so it can seed obligations whose start date is older than the Core's 31-day creation limit (realistic aged state). `@BeforeEach` calls `reset()` and seeds the configured user (and another user, for scoping tests).
* Fixed clock: a controller test that depends on time declares a nested `@TestConfiguration(proxyBeanMethods = false)` class with a `@Bean @Primary Clock` fixed at a known instant (`Clock.fixed(...)`) using the `America/Mexico_City` zone (e.g. `OperationControllerTest.FixedClockConfiguration`). Name the bean anything other than `clock` (e.g. `fixedClock`): Spring Boot forbids overriding the production `clock` bean by name, so `@Primary` makes the test bean win instead.
* JSON responses are parsed with Jackson 3 (`tools.jackson.databind.json.JsonMapper`).
* Cover: the happy path (status, `Location` header where one exists, body, persisted row), a missing required field answering 400, another user's resource answering 404, and one representative 409 with its `code`. Do not re-test every Core rule through HTTP; those belong to the Core tests.
* Caveat: if contexts are ever shared through a base class with an inherited `@DynamicPropertySource`, do not keep the database in a `static @TempDir`: JUnit deletes it after the first subclass while the cached context still points at it. Use a JVM-scoped temp directory instead.

---

## Naming

* Use case tests: `execute_should<Outcome>When<Condition>`, e.g. `execute_shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist`.
* Domain and infrastructure tests: a descriptive present-tense sentence, e.g. `accountStartsActiveWithZeroBalance`, `doesNotFindAccountWhenUserIdDoesNotMatch`.
* Keep one naming style per test class.

---

## Quality Rules

* **Derive tests from the rules, not from the implementation.** Each rule in `domain.md` and `application.md` touched by a change needs tests for both the accepted and the rejected case.
* Cover boundaries: zero amounts, amount equal to the balance, `page = 1`, `pageSize = 100` and `101`, `from == to`.
* Assert the **specific** exception type (`assertThrows(IllegalArgumentException.class, ...)`), never `Exception.class`.
* Business-rule rejections assert `BusinessRuleViolationException` and its `code()` (and message).
* Assert observable results and port interactions that are part of the contract. Do not assert private details.
* Avoid tautological tests: do not only assert values the fake itself returned, and do not stop at `assertNotNull`.
* Tests are deterministic: fixed dates (`LocalDate.of(2026, 8, 20)`), amounts built with `Money.ofCents(...)`, and no dependence on execution order or on the current time.
* Tests are independent: no mutable state shared between test methods.
* A bug fix includes a test that fails without the fix.
* Never weaken or delete an existing test to make a change pass. If an existing test is wrong, explain why in the change description.
