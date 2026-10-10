# Git Guidelines

## Branching Strategy

The project follows a lightweight GitHub Flow strategy.

### Main branch

* `main` is the primary and stable branch.
* Never push directly to `main`.
* All changes must be integrated through a Pull Request.
* `main` should always contain a functional and buildable version of the project.

### Feature branches

Create one branch for each functional or technical change.

Naming convention:

```text
feature/<short-description>
```

Examples:

```text
feature/crear-cuenta
feature/registrar-gasto
feature/registrar-transferencia
feature/sqlite-persistence
feature/transaction-manager
```

### Bug fixes

Use:

```text
fix/<short-description>
```

Examples:

```text
fix/negative-account-balance
fix/duplicate-category
fix/reversal-validation
```

### Refactoring

Use:

```text
refactor/<short-description>
```

Examples:

```text
refactor/financial-operation-model
refactor/repository-contracts
```

## Pull Requests

* Feature, fix, and refactor branches must be merged through a Pull Request.
* The Pull Request should describe the purpose of the change.
* Do not merge changes that cause the project to stop compiling or that break existing tests.
* After merging a branch, the branch should be deleted.

## Commit Convention

The project follows Conventional Commits.

Format:

```text
<type>(<scope>): <description>
```

### Allowed types

* `feat`: New functionality.
* `fix`: Bug correction.
* `refactor`: Structural change without changing behavior.
* `test`: Adding or modifying tests.
* `docs`: Documentation changes.
* `build`: Changes to Maven, dependencies, or build configuration.
* `chore`: Maintenance changes.

### Examples

```text
feat(account): add account aggregate

feat(category): implement category creation

feat(operation): add expense registration

test(account): add balance invariant tests

fix(account): prevent negative balance

refactor(operation): extract financial operation hierarchy

docs(domain): update aggregate decisions
```

## Milestone Tags

Milestone tags represent stable project checkpoints and architecture boundaries. They are not product releases.

Format:

```text
milestone/<name>
```

### Current Completed Milestone
* `milestone/core-mvp-complete`: Marks the complete and validated implementation of the Core backend (Domain, Application use cases, Output ports, Query ports, CQRS read models, JDBC persistence adapters, ThreadLocal transaction management, SQLite schema, and unit/integration test suite).

### Examples of Possible Future Milestones
```text
milestone/ios-foundation-complete
milestone/entry-layer-complete
milestone/mvp-integration-complete
```

## Product Releases (Semantic Versioning)

Product releases follow Semantic Versioning:

```text
v<major>.<minor>.<patch>
```

* **PATCH**: Backward-compatible bug fixes.
* **MINOR**: Backward-compatible new functionality.
* **MAJOR**: Breaking changes.

### First Release Target
The first product release is expected to be:

```text
v1.0.0
```

**`v1.0.0` MUST NOT be created yet.**

`v1.0.0` will represent the first fully usable end-to-end product, requiring:

```text
iOS Application
      +
Entry / Delivery Layer (REST API / Controllers)
      +
Core Backend (milestone/core-mvp-complete)
```

## Tag Immutability

Milestone and release tags are strictly **immutable**.

If an issue or bug is discovered after a tag is created:
1. Do **NOT** move or delete the existing tag.
2. Create a corrective commit on a branch.
3. Create a new appropriate tag when a new checkpoint is reached.

## Copilot & AI Agent Commit Restrictions

The developer performs all Git commits, tags, and pushes manually.

Copilot / AI Agents must **NOT**:
* Create commits
* Amend commits
* Push commits to remotes
* Create or move Git tags

unless explicitly instructed in a future task.

## General Rules

* Keep commits focused on a single logical change.
* Avoid mixing unrelated changes in the same commit.
* Use concise descriptions written in the imperative form.
* Branch names should be lowercase and use hyphens.
* Commit scopes should use the relevant domain or technical component.
* Do not create `develop`, `release`, or `hotfix` branches unless the project's complexity later justifies them.
