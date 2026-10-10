-- Obligation archive date: adds obligations.archived_on (ISO-8601 calendar date, e.g. 2026-10-10) to an existing
-- database. It holds the day the obligation was archived, in the user's time zone. Existing rows get NULL, including
-- obligations archived before this migration, whose archive date is unknown.
-- Additive only. Apply once, by hand, with the application stopped. New databases use schema.sql instead.
-- Runs in a single transaction: run with -bail (see below) and, if any statement fails, nothing is applied.
--
-- How to apply:
--   1. Stop the application and copy the database file (e.g. cp /path/to/db /path/to/db.bak) before running it.
--   2. Run it with -bail, exactly:
--        sqlite3 -bail /path/to/db < src/main/resources/db/migrations/004-obligation-archived-on.sql
-- -bail makes the first failing statement stop the script and roll the transaction back.
--
-- Legacy case-variant names: obligation names are now unique per user ignoring case, but this migration does not
-- touch existing rows, so a database may already hold names that differ only in case (e.g. "Luz" and "luz"). They
-- keep working (a change that keeps the name is not rejected); renaming one of them to another case variant is
-- rejected with OBLIGATION_NAME_ALREADY_EXISTS. To find them (ASCII letters only: SQLite's lower() does not fold
-- accented letters, so pairs like "Educación"/"EDUCACIÓN" are not listed):
--   SELECT user_id, lower(name), COUNT(*) FROM obligations GROUP BY user_id, lower(name) HAVING COUNT(*) > 1;

BEGIN;

ALTER TABLE obligations ADD COLUMN archived_on TEXT;

COMMIT;
