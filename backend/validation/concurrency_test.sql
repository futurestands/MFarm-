-- =====================================================================
-- MFarm Stage D — Two-Connection Concurrency & Cursor Pagination Test
-- =====================================================================

\set ON_ERROR_STOP 1

-- Connection A Session Initialization
SELECT id AS farm_id FROM farms LIMIT 1 \gset
\echo 'Farm ID selected for concurrency test:' :farm_id

-- Let's test cursor pagination using per-farm transactional revision (`server_revision` / `mutation_revision`)
SELECT mutation_id, mutation_revision, entity_type, operation_type
FROM server_mutations
WHERE farm_id = :'farm_id' AND mutation_revision > 0
ORDER BY mutation_revision ASC;
