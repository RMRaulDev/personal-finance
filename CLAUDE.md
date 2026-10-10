# Personal Finance Backend

Project context, architecture, and rules live in `.github/` and are shared with GitHub Copilot:

@.github/copilot-instructions.md
@.github/instructions/architecture.md
@.github/instructions/git.md

Read the other guides in `.github/instructions/` (`java.md`, `domain.md`, `application.md`, `testing.md`) before working on the area they cover.

## Commands

* Build and run all tests: `mvn test`
* Run a single test class: `mvn test -Dtest=RegisterExpenseTest`
* Run the app: `PERSONAL_FINANCE_SQLITE_URL=jdbc:sqlite:/path/to/db PERSONAL_FINANCE_SINGLE_USER_ID=<uuid> mvn spring-boot:run` (the canonical `PERSONALFINANCE_SQLITE_URL` and `--personal-finance.sqlite.url=...` also work; `PERSONAL_FINANCE_TIME_ZONE` optionally overrides the default `America/Mexico_City`). Both the SQLite URL and the single user id are required. The database must be initialized first from `src/main/resources/db/schema.sql`; the app does not create the schema or the user.
  * A fresh database is initialized from `schema.sql`. An existing database must first apply, in order and by hand with the app stopped, the scripts from `src/main/resources/db/migrations/` it is missing (each runs as a single transaction).
  * Provisioning the single user (once per database):
    1. `sqlite3 /path/to/db < src/main/resources/db/schema.sql`
    2. `sqlite3 /path/to/db "INSERT INTO users (id) VALUES (lower('<uuid>'));"` (the id must be stored lowercase)
    3. run the app with `PERSONAL_FINANCE_SINGLE_USER_ID=<uuid>`
  * Caution: if the path does not exist, SQLite creates an empty database file on the first request, without the schema, and that request answers 500 ("no such table: users"). Check the path carefully.

## Development Workflow with Agents

Project subagents live in `.claude/agents/`. Hooks in each agent's frontmatter (scripts in `.claude/hooks/`) restrict which files it may edit and, for the reviewer and docs agents, which Bash commands it may run.

### Full flow (default for any change to production code)

1. **`code-implementer`** implements the change in `src/main`. If the change intentionally alters behavior that existing tests assert, it leaves those tests failing and lists them in its report.
2. **`test-writer`** writes new tests and updates the existing tests listed in step 1. Any other failing test is reported as a bug and goes back to step 1.
3. **`code-reviewer`** reviews the diff. Findings marked Blocking or Important go back to step 1 or 2.
4. **`docs-maintainer`** brings the documentation in line with the change.
5. `mvn test` must pass.

### Short flow (trivial changes only)

Use it only when the change adds no rule and changes no behavior: renames, message wording, formatting, dead-code removal, or a comment fix.

1. **`code-implementer`** makes the change. `mvn test` must pass without touching any test.
2. **`code-reviewer`** reviews the diff and confirms it is trivial. If it is not, switch to the full flow.
3. If the change renames or moves a public class, method, or file, run **`docs-maintainer`** too.

If in doubt, use the full flow.

The developer creates all commits, tags, and pushes manually (see `git.md`).
