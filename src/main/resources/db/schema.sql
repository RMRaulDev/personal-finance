PRAGMA foreign_keys = ON;

CREATE TABLE users (
    id         TEXT PRIMARY KEY NOT NULL,
    created_at TEXT NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE accounts (
    id         TEXT    PRIMARY KEY NOT NULL,
    user_id    TEXT    NOT NULL,
    name       TEXT    NOT NULL,
    balance    INTEGER NOT NULL    DEFAULT 0 CHECK (balance >= 0),
    status     TEXT    NOT NULL,
    created_at TEXT    NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (user_id, name)
);

CREATE TABLE categories (
    id         TEXT PRIMARY KEY NOT NULL,
    user_id    TEXT NOT NULL,
    name       TEXT NOT NULL,
    type       TEXT NOT NULL,
    status     TEXT NOT NULL,
    created_at TEXT NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (user_id, name)
);

CREATE TABLE income_operations (
    id             TEXT    PRIMARY KEY NOT NULL,
    account_id     TEXT    NOT NULL,
    category_id    TEXT    NOT NULL,
    amount         INTEGER NOT NULL    CHECK (amount > 0),
    operation_date TEXT    NOT NULL,
    status         TEXT    NOT NULL,
    created_at     TEXT    NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (account_id)  REFERENCES accounts (id)   ON DELETE RESTRICT,
    FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE TABLE expense_operations (
    id             TEXT    PRIMARY KEY NOT NULL,
    account_id     TEXT    NOT NULL,
    category_id    TEXT    NOT NULL,
    amount         INTEGER NOT NULL    CHECK (amount > 0),
    operation_date TEXT    NOT NULL,
    status         TEXT    NOT NULL,
    created_at     TEXT    NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (account_id)  REFERENCES accounts (id)   ON DELETE RESTRICT,
    FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE TABLE transfer_operations (
    id                TEXT    PRIMARY KEY NOT NULL,
    source_account_id TEXT    NOT NULL,
    target_account_id TEXT    NOT NULL,
    amount            INTEGER NOT NULL    CHECK (amount > 0),
    operation_date    TEXT    NOT NULL,
    created_at        TEXT    NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),

    FOREIGN KEY (source_account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    FOREIGN KEY (target_account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CHECK (source_account_id != target_account_id)
);

-- original_operation_id has no FK because it references either income_operations or expense_operations (polymorphic).
-- Application is responsible for validating the reference.
CREATE TABLE reversals (
    id                     TEXT PRIMARY KEY NOT NULL,
    original_operation_id  TEXT NOT NULL    UNIQUE,
    cancelled_at           TEXT NOT NULL,
    created_at             TEXT NOT NULL    DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

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

CREATE INDEX idx_income_operations_account_date ON income_operations (account_id, operation_date DESC, id DESC);
CREATE INDEX idx_income_operations_category     ON income_operations (category_id);

CREATE INDEX idx_expense_operations_account_date ON expense_operations (account_id, operation_date DESC, id DESC);
CREATE INDEX idx_expense_operations_category     ON expense_operations (category_id);

CREATE INDEX idx_transfer_operations_source_date ON transfer_operations (source_account_id, operation_date DESC, id DESC);
CREATE INDEX idx_transfer_operations_target_date ON transfer_operations (target_account_id, operation_date DESC, id DESC);

CREATE INDEX idx_obligations_user_status ON obligations (user_id, status);
CREATE INDEX idx_obligations_account     ON obligations (account_id);
CREATE INDEX idx_obligations_category    ON obligations (category_id);
