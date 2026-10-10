# Java Guidelines

## Java Version

* The project uses **OpenJDK 26.0.1**.
* Use Java 26 language features and standard library APIs when they provide a clear benefit.
* Do not introduce APIs or language features that require a newer Java version.
* Do not write code assuming Java 21 or an older Java version unless there is a specific compatibility requirement.

## General Style

* Follow standard Java naming conventions.
* Use `PascalCase` for classes, interfaces, records, and enums.
* Use `camelCase` for variables, methods, and parameters.
* Use `UPPER_SNAKE_CASE` for constants.
* Use descriptive names instead of abbreviations.
* Keep classes and methods focused on a single responsibility.
* Prefer readable and explicit code over unnecessarily clever implementations.
* Avoid premature abstractions.

## Types

* Prefer immutable types whenever practical.
* Use Java's standard types when they adequately represent the concept.
* Prefer `UUID` for entity identifiers.
* Use `LocalDate` for dates that represent a calendar date without a time or timezone.
* Use `Instant` for timestamps representing a specific moment in time.
* Use `Optional<T>` for method return values that may legitimately have no result.
* Do not use `Optional<T>` for fields, method parameters, or entity properties unless there is a specific reason.
* Do not use `null` to represent an expected absence when `Optional<T>` can express the contract more clearly.

## Classes and Interfaces

* Prefer interfaces when defining application-level contracts or ports.
* Implementations should not expose unnecessary public methods.
* Keep constructors and factory methods consistent with the invariants of the object being created.
* Objects should not be created in partially valid states.
* Prefer composition to inheritance unless inheritance clearly represents the domain relationship.
* Do not introduce inheritance solely to reuse implementation code.

## Records

* Use Java `record` when representing immutable data that does not require identity or mutable lifecycle.
* Do not use records for entities whose identity and lifecycle are part of the domain.
* Records may be used for immutable application data structures, such as query criteria or command data.

## Enums

* Use `enum` for a finite and well-defined set of values.
* Do not replace simple enums with strings.
* Avoid adding behavior to an enum unless that behavior clearly belongs to the concept represented by the enum.

## Collections

* Prefer interfaces such as `List`, `Set`, and `Map` in declarations.
* Do not expose mutable internal collections directly.
* Prefer immutable collections when the collection should not be modified after creation.
* Do not use collections as a substitute for a domain concept when the concept requires its own behavior or invariants.

## Exceptions

* Use exceptions for exceptional or invalid states, not for normal control flow.
* Use specific exception types that communicate the reason for failure.
* Avoid catching `Exception` unless there is a clear reason to handle all exceptions. Two broad catches are sanctioned:
  * JDBC row mappers may catch `RuntimeException`, only around the in-memory row-to-object mapping, and wrap it in `CorruptedPersistedDataException` preserving the cause.
  * The global `@ExceptionHandler(Exception.class)` in `ApiExceptionHandler`.
* Do not silently swallow exceptions.
* Preserve the original cause when wrapping an exception.
* Domain and application exceptions should communicate business or application-level failures without exposing infrastructure implementation details.

## Equality

* Do not rely on object reference equality for domain entities.
* Entity equality should be based on the entity's identity.
* Value Objects should use value-based equality.
* When using Java classes, implement `equals()` and `hashCode()` consistently.
* Do not include mutable properties in equality when doing so could make equality unstable.

## Null Safety

* Avoid `null` whenever a more explicit representation is available.
* Validate required arguments at object boundaries.
* Do not add defensive null checks everywhere without understanding the contract of the method.
* Prefer APIs and types that make invalid states difficult to represent.

## Dependencies

* Prefer the Java Standard Library when it provides the required functionality.
* Do not add an external dependency for functionality that can be implemented clearly with the standard library.
* Before introducing a dependency, consider its maintenance status, complexity, licensing, and actual value to the project.
* Keep infrastructure-specific dependencies isolated from the domain and application layers.
* Spring is allowed only in the `entry` package.

## Code Quality

* Avoid duplicated logic when the duplicated behavior represents the same concept.
* Do not extract abstractions solely to reduce the number of lines of code.
* Prefer small, cohesive methods.
* Avoid methods with excessive parameters; introduce a meaningful object when a group of parameters represents a concept.
* Avoid unnecessary getters and setters, especially in domain entities.
* Do not expose mutable state unnecessarily.
* Prefer behavior-oriented methods to direct manipulation of internal state.

## Documentation

* Write documentation when it explains a non-obvious design decision or constraint.
* Do not add comments that merely restate what the code already expresses.
* Keep comments and documentation synchronized with the implementation.
* Public APIs should have clear contracts when their behavior is not obvious from their signatures.
