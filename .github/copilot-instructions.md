# Personal Finance Backend

## Project Context

This repository contains the backend of a personal finance management application.

The backend is designed to be **decoupled from the frontend**, allowing different clients or interfaces to consume it without coupling the business logic to a specific presentation technology.

The project is being developed as an **MVP**, with the intention of keeping the implementation simple while maintaining a structure that can evolve as the application grows.

### Technology Stack

* **Language:** Java
* **Java Version:** OpenJDK 26.0.1
* **Build Tool:** Maven
* **Database:** SQLite
* **Entry layer:** Spring Boot 4.1.1 (Spring MVC)

### Architectural Approach

The project combines:

* Domain-Driven Design (DDD)
* Clean Architecture
* Ports and Adapters

The main architectural layers are:

```text
Domain
Application
Infrastructure
Entry
```

The Domain contains the business model and rules.

The Application layer contains use cases and application orchestration.

The Infrastructure layer contains technical implementations such as persistence and database access.

The Entry layer adapts delivery mechanisms (HTTP via Spring Boot) to the application use cases and wires the Core.

### Domain Overview

The application manages personal finances through concepts such as:

* Users
* Accounts
* Categories
* Financial operations
* Obligations (payment commitments, their recurrence, and occurrence resolutions)
* Reversals
* Money

Financial operations include:

* Income
* Expense
* Transfer

The current MVP uses MXN as its currency.

---

# Instruction Index

Detailed project instructions are organized into the following files:

* [`git.md`](./instructions/git.md) — Git workflow and branching strategy.
* [`java.md`](./instructions/java.md) — Java coding conventions and language guidelines.
* [`architecture.md`](./instructions/architecture.md) — Architectural boundaries and dependency rules.
* [`domain.md`](./instructions/domain.md) — Domain model and business invariants.
* [`application.md`](./instructions/application.md) — Application layer and use case guidelines.
* [`testing.md`](./instructions/testing.md) — Testing strategy, conventions, and quality rules.