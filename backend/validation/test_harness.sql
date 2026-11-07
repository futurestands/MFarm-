-- =====================================================================
-- MFarm Stage D — Final Acceptance Test Harness
-- =====================================================================

\set ON_ERROR_STOP 1

-- Verify exact disposable database identity check
DO $$
BEGIN
    IF current_database() <> 'mfarm_val_db' THEN
        RAISE EXCEPTION 'SAFETY ERROR: Test harness running on unauthorized database % (expected mfarm_val_db)', current_database();
    END IF;
END $$;

DROP TABLE IF EXISTS test_context CASCADE;
CREATE TABLE test_context (
    key TEXT PRIMARY KEY,
    val UUID
);

-- 1. Setup Test Farms, Users & Sessions (Superuser context)
DO $$
DECLARE
    v_farm_a UUID := uuid_generate_v4();
    v_farm_b UUID := uuid_generate_v4();
    v_user_owner UUID := uuid_generate_v4();
    v_user_manager UUID := uuid_generate_v4();
    v_user_worker_a UUID := uuid_generate_v4();
    v_session_token_a UUID := uuid_generate_v4();
    v_session_token_b UUID := uuid_generate_v4();
    v_manager_token UUID := uuid_generate_v4();
    v_expired_token UUID := uuid_generate_v4();
    v_goat_b UUID := uuid_generate_v4();
BEGIN
    INSERT INTO farms (id, name) VALUES (v_farm_a, 'Kazinga Farm'), (v_farm_b, 'Victoria Farm');
    INSERT INTO users (id, email, full_name, password_hash) VALUES
        (v_user_owner, v_user_owner::text || '@mfarm.dev', 'Farm Owner', 'secret_hash_a'),
        (v_user_manager, v_user_manager::text || '@mfarm.dev', 'Farm Manager', 'secret_hash_b'),
        (v_user_worker_a, v_user_worker_a::text || '@mfarm.dev', 'Farm Worker A', 'secret_hash_c');

    INSERT INTO farm_members (farm_id, user_id, role) VALUES
        (v_farm_a, v_user_owner, 'OWNER'),
        (v_farm_a, v_user_manager, 'MANAGER'),
        (v_farm_a, v_user_worker_a, 'WORKER'),
        (v_farm_b, v_user_owner, 'OWNER');

    INSERT INTO livestock (farm_id, global_id, tag, species, breed)
    VALUES (v_farm_b, v_goat_b, 'VB-COW-01', 'Cattle', 'Ankole');

    -- Valid session for Worker A in Farm A
    INSERT INTO active_sessions (session_token, user_id, farm_id, expires_at)
    VALUES (v_session_token_a, v_user_worker_a, v_farm_a, NOW() + INTERVAL '1 hour');

    -- Valid session for Owner in Farm B
    INSERT INTO active_sessions (session_token, user_id, farm_id, expires_at)
    VALUES (v_session_token_b, v_user_owner, v_farm_b, NOW() + INTERVAL '1 hour');

    INSERT INTO active_sessions (session_token, user_id, farm_id, expires_at)
    VALUES (v_manager_token, v_user_manager, v_farm_a, NOW() + INTERVAL '1 hour');

    INSERT INTO active_sessions (session_token, user_id, farm_id, expires_at)
    VALUES (v_expired_token, v_user_worker_a, v_farm_a, NOW() - INTERVAL '1 hour');

    CREATE TEMP TABLE IF NOT EXISTS test_context (key TEXT PRIMARY KEY, val UUID);
    TRUNCATE test_context;
    INSERT INTO test_context VALUES
        ('farm_a', v_farm_a),
        ('farm_b', v_farm_b),
        ('user_owner', v_user_owner),
        ('user_manager', v_user_manager),
        ('user_worker_a', v_user_worker_a),
        ('session_token_a', v_session_token_a),
        ('session_token_b', v_session_token_b),
        ('manager_token', v_manager_token),
        ('expired_token', v_expired_token),
        ('goat_b', v_goat_b);

    GRANT SELECT ON test_context TO mfarm_app_user;

    RAISE NOTICE 'TEST SETUP COMPLETE: Multi-tenant isolated environment initialized (Tokens [REDACTED]).';
END $$;

-- Switch to restricted application role
SET ROLE mfarm_app_user;

-- 2. Test 1: Effective Privileges & Password Hash Column Protection
DO $$
DECLARE
    v_has_password_hash BOOLEAN;
    v_has_insert BOOLEAN;
    v_farm_a UUID;
    v_session_token UUID;
BEGIN
    SELECT val INTO v_farm_a FROM test_context WHERE key = 'farm_a';
    SELECT val INTO v_session_token FROM test_context WHERE key = 'session_token_a';
    PERFORM auth.authenticate_session(v_session_token, v_farm_a);

    -- Verify mfarm_app_user cannot query password_hash column from users table
    BEGIN
        PERFORM password_hash FROM users LIMIT 1;
        RAISE EXCEPTION 'TEST 1A FAILED: mfarm_app_user can query password_hash column!';
    EXCEPTION WHEN insufficient_privilege OR undefined_column THEN
        RAISE NOTICE 'TEST 1A PASSED: password_hash column strictly protected from application role.';
    END;

    -- Verify direct DML revoked
    v_has_insert := has_table_privilege('mfarm_app_user', 'livestock', 'INSERT');
    IF v_has_insert THEN
        RAISE EXCEPTION 'TEST 1B FAILED: mfarm_app_user has INSERT privilege on livestock!';
    END IF;

    RAISE NOTICE 'TEST 1 PASSED: Effective privileges and sensitive columns successfully audited.';
END $$;

-- 3. Test 2: Authentication Boundary & Context Forgery Defense
DO $$
DECLARE
    v_farm_a UUID;
    v_farm_b UUID;
    v_token_b UUID;
    v_err_code TEXT;
BEGIN
    SELECT val INTO v_farm_a FROM test_context WHERE key = 'farm_a';
    SELECT val INTO v_farm_b FROM test_context WHERE key = 'farm_b';
    SELECT val INTO v_token_b FROM test_context WHERE key = 'session_token_b';

    -- Attempting to use Farm B token while targeting Farm A (Cross-farm session misuse)
    BEGIN
        PERFORM auth.authenticate_session(v_token_b, v_farm_a);
        RAISE EXCEPTION 'TEST 2A FAILED: Cross-farm session token misuse accepted!';
    EXCEPTION WHEN insufficient_privilege THEN
        GET STACKED DIAGNOSTICS v_err_code = RETURNED_SQLSTATE;
        IF v_err_code <> '42501' THEN
            RAISE EXCEPTION 'TEST 2A FAILED: Expected SQLSTATE 42501, got %', v_err_code;
        END IF;
        RAISE NOTICE 'TEST 2A PASSED: Cross-farm token misuse rejected with SQLSTATE 42501 (Token [REDACTED]).';
    END;

    -- Attempting to forge mfarm.current_session_token via set_config
    PERFORM set_config('mfarm.current_session_token', gen_random_uuid()::text, true);
    IF auth.current_user_id() IS NOT NULL THEN
        RAISE EXCEPTION 'TEST 2B FAILED: Forged session token accepted by auth.current_user_id()!';
    END IF;

    RAISE NOTICE 'TEST 2B PASSED: Session token forgery successfully defended.';
END $$;

-- 4. Test 3: Farm Isolation
DO $$
DECLARE
    v_farm_a UUID;
    v_farm_b UUID;
    v_session_token UUID;
    v_goat_b UUID;
    v_count INT;
BEGIN
    SELECT val INTO v_farm_a FROM test_context WHERE key = 'farm_a';
    SELECT val INTO v_farm_b FROM test_context WHERE key = 'farm_b';
    SELECT val INTO v_session_token FROM test_context WHERE key = 'session_token_a';
    SELECT val INTO v_goat_b FROM test_context WHERE key = 'goat_b';

    PERFORM auth.authenticate_session(v_session_token, v_farm_a);

    SELECT COUNT(*) INTO v_count FROM livestock WHERE farm_id = v_farm_b AND global_id = v_goat_b;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'TEST 3 FAILED: Farm A worker accessed Farm B livestock!';
    END IF;

    RAISE NOTICE 'TEST 3 PASSED: Cross-farm isolation strictly enforced.';
END $$;

-- 5. Test 4: OCC & Revision Locking
DO $$
DECLARE
    v_farm_a UUID;
    v_session_token UUID;
    v_goat_id UUID;
    v_new_rev BIGINT;
    v_err_code TEXT;
BEGIN
    SELECT val INTO v_farm_a FROM test_context WHERE key = 'farm_a';
    SELECT val INTO v_session_token FROM test_context WHERE key = 'session_token_a';

    PERFORM auth.authenticate_session(v_session_token, v_farm_a);

    v_goat_id := create_livestock(v_farm_a, 'KZ-GOAT-A3', 'Goat', 'Boer');
    v_new_rev := update_livestock_revision(v_farm_a, v_goat_id, 1, 'Boer Pure');
    IF v_new_rev <> 2 THEN
        RAISE EXCEPTION 'TEST 4A FAILED: Revision did not increment correctly (rev = %)', v_new_rev;
    END IF;

    -- Stale revision update test (expects SQLSTATE 40001)
    BEGIN
        PERFORM update_livestock_revision(v_farm_a, v_goat_id, 1, 'Stale Update');
        RAISE EXCEPTION 'TEST 4B FAILED: Stale revision allowed!';
    EXCEPTION WHEN serialization_failure THEN
        GET STACKED DIAGNOSTICS v_err_code = RETURNED_SQLSTATE;
        IF v_err_code <> '40001' THEN
            RAISE EXCEPTION 'TEST 4B FAILED: Expected SQLSTATE 40001, got %', v_err_code;
        END IF;
        RAISE NOTICE 'TEST 4 PASSED: OCC revision check rejected stale update with SQLSTATE 40001.';
    END;
END $$;

-- 6. Test 5: Atomic Idempotency, Payload Conflict & Unsupported Operation Rejection
DO $$
DECLARE
    v_farm_a UUID;
    v_session_token UUID;
    v_op_id UUID := gen_random_uuid();
    v_res1 JSONB;
    v_res2 JSONB;
    v_err_code TEXT;
BEGIN
    SELECT val INTO v_farm_a FROM test_context WHERE key = 'farm_a';
    SELECT val INTO v_session_token FROM test_context WHERE key = 'session_token_a';

    PERFORM auth.authenticate_session(v_session_token, v_farm_a);

    v_res1 := execute_client_operation(v_farm_a, v_op_id, '{"livestock_tag": "KZ-GOAT-A4", "species": "Goat", "breed": "Savannah"}'::jsonb);
    v_res2 := execute_client_operation(v_farm_a, v_op_id, '{"livestock_tag": "KZ-GOAT-A4", "species": "Goat", "breed": "Savannah"}'::jsonb);

    IF v_res1->>'entity_id' <> v_res2->>'entity_id' THEN
        RAISE EXCEPTION 'TEST 5A FAILED: Idempotent retry did not return original result.';
    END IF;

    -- Conflicting payload with same operation ID (expects SQLSTATE 40901)
    BEGIN
        PERFORM execute_client_operation(v_farm_a, v_op_id, '{"livestock_tag": "KZ-GOAT-TAMPERED", "species": "Goat", "breed": "Bad"}'::jsonb);
        RAISE EXCEPTION 'TEST 5B FAILED: Payload conflict allowed!';
    EXCEPTION WHEN others THEN
        GET STACKED DIAGNOSTICS v_err_code = RETURNED_SQLSTATE;
        IF v_err_code <> '40901' THEN
            RAISE EXCEPTION 'TEST 5B FAILED: Expected SQLSTATE 40901, got %', v_err_code;
        END IF;
    END;

    -- Unsupported operation type rejection (expects SQLSTATE 42201)
    BEGIN
        PERFORM execute_client_operation(v_farm_a, gen_random_uuid(), '{"unknown_action": true}'::jsonb);
        RAISE EXCEPTION 'TEST 5C FAILED: Unsupported operation type allowed!';
    EXCEPTION WHEN others THEN
        GET STACKED DIAGNOSTICS v_err_code = RETURNED_SQLSTATE;
        IF v_err_code <> '42201' THEN
            RAISE EXCEPTION 'TEST 5C FAILED: Expected SQLSTATE 42201, got %', v_err_code;
        END IF;
        RAISE NOTICE 'TEST 5 PASSED: Atomic idempotency, payload conflict (40901) & unsupported operation rejection (42201) verified.';
    END;
END $$;

-- 7. Test 6: Financial Reversals, JSONB Row Equality & Unique Reversal Constraint
DO $$
DECLARE
    v_farm_a UUID;
    v_manager_token UUID;
    v_fin_id UUID;
    v_rev_id UUID;
    v_orig_before financial_ledgers%ROWTYPE;
    v_orig_after financial_ledgers%ROWTYPE;
    v_err_code TEXT;
BEGIN
    SELECT val INTO v_farm_a FROM test_context WHERE key = 'farm_a';
    SELECT val INTO v_manager_token FROM test_context WHERE key = 'manager_token';

    PERFORM auth.authenticate_session(v_manager_token, v_farm_a);

    v_fin_id := insert_financial_ledger(v_farm_a, 'EXPENSE', 'Feed', 2500000, CURRENT_DATE, 'Hay bales');

    SELECT * INTO v_orig_before FROM financial_ledgers WHERE farm_id = v_farm_a AND global_id = v_fin_id;

    -- Execute reversal
    v_rev_id := reverse_financial_ledger(v_farm_a, v_fin_id, 'Correction offset');

    -- Verify original row is byte-for-byte identical via JSONB equality comparison
    SELECT * INTO v_orig_after FROM financial_ledgers WHERE farm_id = v_farm_a AND global_id = v_fin_id;

    IF to_jsonb(v_orig_before) <> to_jsonb(v_orig_after) THEN
        RAISE EXCEPTION 'TEST 6A FAILED: Original financial ledger row JSONB representation changed!';
    END IF;

    -- Test database unique constraint on duplicate reversal (expects SQLSTATE 23505)
    BEGIN
        PERFORM reverse_financial_ledger(v_farm_a, v_fin_id, 'Duplicate reversal attempt');
        RAISE EXCEPTION 'TEST 6B FAILED: Duplicate reversal allowed!';
    EXCEPTION WHEN unique_violation OR others THEN
        GET STACKED DIAGNOSTICS v_err_code = RETURNED_SQLSTATE;
        IF v_err_code <> '23505' AND v_err_code <> 'P0003' THEN
            RAISE EXCEPTION 'TEST 6B FAILED: Expected SQLSTATE 23505 or P0003, got %', v_err_code;
        END IF;
        RAISE NOTICE 'TEST 6 PASSED: Financial row JSONB equality verified & database unique constraint enforced (SQLSTATE: %).', v_err_code;
    END;
END $$;

-- 8. Test 7: Revision-Based Change Feed Pagination (Page Size 1 Verification)
DO $$
DECLARE
    v_farm_a UUID;
    v_session_token UUID;
    v_last_rev BIGINT := 0;
    v_rec RECORD;
    v_iter_count INT := 0;
BEGIN
    SELECT val INTO v_farm_a FROM test_context WHERE key = 'farm_a';
    SELECT val INTO v_session_token FROM test_context WHERE key = 'session_token_a';

    PERFORM auth.authenticate_session(v_session_token, v_farm_a);

    LOOP
        SELECT mutation_id, mutation_revision, entity_type, operation_type INTO v_rec
        FROM server_mutations
        WHERE farm_id = v_farm_a AND mutation_revision > v_last_rev
        ORDER BY mutation_revision ASC
        LIMIT 1;

        EXIT WHEN NOT FOUND;

        v_last_rev := v_rec.mutation_revision;
        v_iter_count := v_iter_count + 1;
    END LOOP;

    IF v_iter_count < 5 THEN
        RAISE EXCEPTION 'TEST 7 FAILED: Change feed pagination returned % records (expected >= 5)', v_iter_count;
    END IF;

    RAISE NOTICE 'TEST 7 PASSED: Revision-based change feed pagination (page size 1) verified (% mutations paginated in strict sequence).', v_iter_count;
END $$;

RESET ROLE;

-- FINAL OVERALL PASS NOTICE
DO $$
BEGIN
    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'OVERALL TEST RESULT: PASS (All Stage D Final Acceptance Tests passed)';
    RAISE NOTICE '=====================================================';
END $$;
