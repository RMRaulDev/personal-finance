-- Optional operation category: makes income_operations.category_id and expense_operations.category_id nullable
-- (NULL means the operation has no category) in an existing database. The foreign keys to categories stay.
-- Apply once, by hand, with the application stopped. New databases use schema.sql instead.
-- Runs in a single transaction: run with -bail (see below) and, if any statement fails, nothing is applied.
--
-- SQLite cannot drop NOT NULL, so both tables are rebuilt with SQLite's documented procedure: create the new table,
-- copy every row (ids, all columns, and created_at unchanged), drop the old table, rename the new one, and recreate
-- its indexes. Foreign keys must be OFF during the rebuild, otherwise dropping expense_operations would act on
-- occurrence_resolutions.expense_id. PRAGMA foreign_keys has no effect inside a transaction, so it is turned OFF
-- before BEGIN and back ON after COMMIT. Before COMMIT, PRAGMA foreign_key_check lists any violation, and the guard
-- table makes the script fail if there is one.
--
-- How to apply:
--   1. Stop the application and copy the database file (e.g. cp /path/to/db /path/to/db.bak) before running it.
--   2. Run it with -bail, exactly:
--        sqlite3 -bail /path/to/db < src/main/resources/db/migrations/003-optional-operation-category.sql
-- -bail is required, not optional: it makes the first failing statement stop the script and roll the transaction
-- back. Without it, sqlite3 keeps going, so a failed INSERT ... SELECT would be followed by DROP TABLE and COMMIT,
-- losing the table's rows.

PRAGMA foreign_keys = OFF;

BEGIN;

CREATE TABLE income_operations_new (
    id             TEXT    PRIMARY KEY NOT NULL,
    account_id     TEXT    NOT NULL,
    category_id    TEXT,
    amount         INTEGER NOT NULL    CHECK (amount > 0),
    operation_date TEXT    NOT NULL,
    status         TEXT    NOT NULL,
    created_at     TEXT    NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (account_id)  REFERENCES accounts (id)   ON DELETE RESTRICT,
    FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

INSERT INTO income_operations_new (id, account_id, category_id, amount, operation_date, status, created_at)
SELECT id, account_id, category_id, amount, operation_date, status, created_at FROM income_operations;

DROP TABLE income_operations;

ALTER TABLE income_operations_new RENAME TO income_operations;

CREATE INDEX idx_income_operations_account_date ON income_operations (account_id, operation_date DESC, id DESC);
CREATE INDEX idx_income_operations_category     ON income_operations (category_id);

CREATE TABLE expense_operations_new (
    id             TEXT    PRIMARY KEY NOT NULL,
    account_id     TEXT    NOT NULL,
    category_id    TEXT,
    amount         INTEGER NOT NULL    CHECK (amount > 0),
    operation_date TEXT    NOT NULL,
    status         TEXT    NOT NULL,
    created_at     TEXT    NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (account_id)  REFERENCES accounts (id)   ON DELETE RESTRICT,
    FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

INSERT INTO expense_operations_new (id, account_id, category_id, amount, operation_date, status, created_at)
SELECT id, account_id, category_id, amount, operation_date, status, created_at FROM expense_operations;

DROP TABLE expense_operations;

ALTER TABLE expense_operations_new RENAME TO expense_operations;

CREATE INDEX idx_expense_operations_account_date ON expense_operations (account_id, operation_date DESC, id DESC);
CREATE INDEX idx_expense_operations_category     ON expense_operations (category_id);

PRAGMA foreign_key_check;

CREATE TEMP TABLE migration_003_foreign_key_guard (violations INTEGER NOT NULL CHECK (violations = 0));
INSERT INTO migration_003_foreign_key_guard SELECT count(*) FROM pragma_foreign_key_check;
DROP TABLE migration_003_foreign_key_guard;

COMMIT;

PRAGMA foreign_keys = ON;
