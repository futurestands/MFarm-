-- Connection A transaction holding lock with timestamp and transaction boundary
\echo [Connection A] START timestamp: :TIMESTAMPTZ
BEGIN;
SELECT current_revision FROM farm_revisions LIMIT 1 FOR UPDATE;
\echo [Connection A] Acquired exclusive row lock on farm_revisions. Holding for 2 seconds...
SELECT pg_sleep(2);
COMMIT;
\echo [Connection A] COMMIT completed at timestamp: :TIMESTAMPTZ
