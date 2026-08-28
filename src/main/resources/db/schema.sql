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

CREATE INDEX idx_income_operations_account_date ON income_operations (account_id, operation_date DESC, id DESC);
CREATE INDEX idx_income_operations_category     ON income_operations (category_id);

CREATE INDEX idx_expense_operations_account_date ON expense_operations (account_id, operation_date DESC, id DESC);
CREATE INDEX idx_expense_operations_category     ON expense_operations (category_id);

CREATE INDEX idx_transfer_operations_source_date ON transfer_operations (source_account_id, operation_date DESC, id DESC);
CREATE INDEX idx_transfer_operations_target_date ON transfer_operations (target_account_id, operation_date DESC, id DESC);
