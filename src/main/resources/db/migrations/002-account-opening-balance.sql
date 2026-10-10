-- Account opening balance: adds accounts.opening_balance (cents) to an existing database.
-- Existing accounts get 0, so their balance stays equal to the sum of their operations.
-- Additive only. Apply once, by hand, with the application stopped. New databases use schema.sql instead.
-- Runs in a single transaction: if any statement fails, nothing is applied.

BEGIN;

ALTER TABLE accounts ADD COLUMN opening_balance INTEGER NOT NULL DEFAULT 0 CHECK (opening_balance >= 0);

COMMIT;
