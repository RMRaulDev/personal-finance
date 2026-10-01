-- Obligations Core: adds the obligations and occurrence_resolutions tables to an existing database.
-- Additive only. Apply once, by hand, with the application stopped. New databases use schema.sql instead.
-- Runs in a single transaction: if any statement fails, nothing is applied.

BEGIN;

-- account_id and category_id use NO ACTION (checked at the end of the statement), not RESTRICT, so deleting a user
-- cascades to accounts, categories, and obligations regardless of the order SQLite processes the cascades.
CREATE TABLE obligations (
    id          TEXT    PRIMARY KEY NOT NULL,
    user_id     TEXT    NOT NULL,
    name        TEXT    NOT NULL,
    amount      INTEGER NOT NULL    CHECK (amount > 0),
    account_id  TEXT    NOT NULL,
    category_id TEXT    NOT NULL,
    frequency   TEXT    NOT NULL,
    start_date  TEXT    NOT NULL,
    end_date    TEXT,
    status      TEXT    NOT NULL,
    created_at  TEXT    NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    FOREIGN KEY (account_id)  REFERENCES accounts (id),
    FOREIGN KEY (category_id) REFERENCES categories (id),
    UNIQUE (user_id, name),
    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE occurrence_resolutions (
    id            TEXT PRIMARY KEY NOT NULL,
    obligation_id TEXT NOT NULL,
    due_date      TEXT NOT NULL,
    status        TEXT NOT NULL,
    expense_id    TEXT UNIQUE,
    resolved_at   TEXT NOT NULL,
    created_at    TEXT NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (obligation_id) REFERENCES obligations (id)        ON DELETE CASCADE,
    FOREIGN KEY (expense_id)    REFERENCES expense_operations (id) ON DELETE RESTRICT,
    UNIQUE (obligation_id, due_date),
    CHECK ((status = 'PAID' AND expense_id IS NOT NULL) OR (status = 'SKIPPED' AND expense_id IS NULL))
);

CREATE INDEX idx_obligations_user_status ON obligations (user_id, status);
CREATE INDEX idx_obligations_account     ON obligations (account_id);
CREATE INDEX idx_obligations_category    ON obligations (category_id);

COMMIT;
