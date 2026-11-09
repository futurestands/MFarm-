-- =====================================================================
-- MFarm Stage D — Final Acceptance Schema
-- =====================================================================

-- Verify exact disposable database identity check
DO $$
BEGIN
    IF current_database() <> 'mfarm_val_db' THEN
        RAISE EXCEPTION 'SAFETY ERROR: Attempted to run validation schema on non-disposable database % (expected mfarm_val_db)', current_database();
    END IF;
END $$;

DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;

CREATE SCHEMA IF NOT EXISTS auth;
REVOKE ALL ON SCHEMA auth FROM PUBLIC;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Farms, Users & Sessions
CREATE TABLE public.farms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    location TEXT,
    currency VARCHAR(10) DEFAULT 'UGX',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.farm_members (
    farm_id UUID REFERENCES public.farms(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    role VARCHAR(50) CHECK (role IN ('OWNER', 'MANAGER', 'WORKER')),
    PRIMARY KEY (farm_id, user_id)
);

-- Active Sessions (Revoked from app role; accessed exclusively via SECURITY DEFINER)
CREATE TABLE public.active_sessions (
    session_token UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '1 hour')
);

REVOKE ALL ON public.active_sessions FROM PUBLIC, mfarm_app_user;

-- Secure Session Authentication Function (Binds session token and farm context strictly)
CREATE OR REPLACE FUNCTION auth.authenticate_session(p_token UUID, p_farm_id UUID)
RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_session_farm UUID;
BEGIN
    SELECT user_id, farm_id INTO v_user_id, v_session_farm
    FROM public.active_sessions
    WHERE session_token = p_token
      AND expires_at > NOW();

    IF v_user_id IS NULL OR v_session_farm <> p_farm_id THEN
        RAISE EXCEPTION 'Unauthorized session, expired token, or cross-farm token misuse.' USING ERRCODE = '42501';
    END IF;

    -- Bind session context securely
    PERFORM set_config('mfarm.current_session_token', p_token::text, true);
    PERFORM set_config('mfarm.current_farm_id', p_farm_id::text, true);
    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

-- Retrieve verified user ID exclusively from session token validation
CREATE OR REPLACE FUNCTION auth.current_user_id()
RETURNS UUID AS $$
DECLARE
    v_token TEXT;
    v_user_id UUID;
BEGIN
    v_token := nullif(current_setting('mfarm.current_session_token', true), '');
    IF v_token IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT user_id INTO v_user_id
    FROM public.active_sessions
    WHERE session_token = v_token::uuid
      AND expires_at > NOW();

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE SET search_path = public, pg_temp;

-- Security Definer Function to Check Membership Role without RLS Recursion
CREATE OR REPLACE FUNCTION auth.get_user_role(p_farm_id UUID)
RETURNS VARCHAR AS $$
BEGIN
    IF nullif(current_setting('mfarm.current_farm_id', true), '')::uuid <> p_farm_id THEN
        RETURN NULL;
    END IF;

    RETURN (
        SELECT role FROM public.farm_members
        WHERE farm_id = p_farm_id
          AND user_id = auth.current_user_id()
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE SET search_path = public, pg_temp;

-- Per-Farm Transactional Revision Counter Table
CREATE TABLE public.farm_revisions (
    farm_id UUID PRIMARY KEY REFERENCES public.farms(id) ON DELETE CASCADE,
    current_revision BIGINT NOT NULL DEFAULT 0
);

-- 2. Livestock
CREATE TABLE public.livestock (
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    global_id UUID DEFAULT uuid_generate_v4(),
    local_id BIGINT,
    tag VARCHAR(100) NOT NULL,
    species VARCHAR(100) NOT NULL,
    breed VARCHAR(100),
    dob DATE,
    sex VARCHAR(20) CHECK (sex IN ('MALE', 'FEMALE', 'UNKNOWN')),
    sire_farm_id UUID,
    sire_id UUID,
    dam_farm_id UUID,
    dam_id UUID,
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SOLD', 'DECEASED')),
    notes TEXT,
    server_revision BIGINT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (farm_id, global_id),
    CONSTRAINT unique_farm_tag UNIQUE (farm_id, tag),
    CONSTRAINT fk_livestock_sire FOREIGN KEY (sire_farm_id, sire_id) REFERENCES public.livestock(farm_id, global_id) ON DELETE SET NULL,
    CONSTRAINT fk_livestock_dam FOREIGN KEY (dam_farm_id, dam_id) REFERENCES public.livestock(farm_id, global_id) ON DELETE SET NULL,
    CONSTRAINT check_sire_same_farm CHECK (sire_farm_id IS NULL OR sire_farm_id = farm_id),
    CONSTRAINT check_dam_same_farm CHECK (dam_farm_id IS NULL OR dam_farm_id = farm_id)
);

-- 3. Financial Ledgers: 100% Immutable, Append-Only with Database-Enforced Unique Reversal Index
CREATE TABLE public.financial_ledgers (
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    global_id UUID DEFAULT uuid_generate_v4(),
    entry_type VARCHAR(20) CHECK (entry_type IN ('INCOME', 'EXPENSE')),
    category VARCHAR(100) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    entry_date DATE NOT NULL,
    notes TEXT,
    reversal_farm_id UUID,
    reversal_reference_id UUID,
    created_by UUID REFERENCES public.users(id),
    server_revision BIGINT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (farm_id, global_id),
    CONSTRAINT fk_financial_reversal FOREIGN KEY (reversal_farm_id, reversal_reference_id) REFERENCES public.financial_ledgers(farm_id, global_id),
    CONSTRAINT check_reversal_same_farm CHECK (reversal_farm_id IS NULL OR reversal_farm_id = farm_id)
);

CREATE UNIQUE INDEX idx_unique_financial_reversal
ON public.financial_ledgers (farm_id, reversal_reference_id)
WHERE reversal_reference_id IS NOT NULL;

-- 4. Permanent Operation Deduplication
CREATE TABLE public.client_operations (
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    operation_id UUID NOT NULL,
    payload_hash TEXT NOT NULL,
    response_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (farm_id, operation_id)
);

-- 5. Commit-Safe Synchronization Change-Feed Stream
CREATE TABLE public.server_mutations (
    mutation_id BIGINT GENERATED ALWAYS AS IDENTITY,
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    mutation_revision BIGINT NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    operation_type VARCHAR(20) CHECK (operation_type IN ('INSERT', 'UPDATE', 'DELETE')),
    payload JSONB NOT NULL,
    committed_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (farm_id, mutation_id)
);

-- Enable RLS on All Tables
ALTER TABLE public.farms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.farm_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.active_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.farm_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.livestock ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.financial_ledgers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.client_operations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.server_mutations ENABLE ROW LEVEL SECURITY;

-- Granular RLS Policies
CREATE POLICY farms_select ON public.farms FOR SELECT USING (auth.get_user_role(id) IS NOT NULL);
CREATE POLICY farms_dml_block ON public.farms FOR ALL USING (false) WITH CHECK (false);

CREATE POLICY farm_members_select ON public.farm_members FOR SELECT USING (auth.get_user_role(farm_id) IS NOT NULL);
CREATE POLICY farm_members_dml_block ON public.farm_members FOR ALL USING (false) WITH CHECK (false);

CREATE POLICY farm_revisions_select ON public.farm_revisions FOR SELECT USING (auth.get_user_role(farm_id) IS NOT NULL);
CREATE POLICY farm_revisions_dml_block ON public.farm_revisions FOR ALL USING (false) WITH CHECK (false);

CREATE POLICY livestock_select ON public.livestock FOR SELECT USING (auth.get_user_role(farm_id) IS NOT NULL);
CREATE POLICY livestock_dml_block ON public.livestock FOR ALL USING (false) WITH CHECK (false);

CREATE POLICY financial_select ON public.financial_ledgers FOR SELECT USING (auth.get_user_role(farm_id) IS NOT NULL);
CREATE POLICY financial_dml_block ON public.financial_ledgers FOR ALL USING (false) WITH CHECK (false);

CREATE POLICY operations_select ON public.client_operations FOR SELECT USING (auth.get_user_role(farm_id) IS NOT NULL);
CREATE POLICY operations_dml_block ON public.client_operations FOR ALL USING (false) WITH CHECK (false);

CREATE POLICY mutations_select ON public.server_mutations FOR SELECT USING (auth.get_user_role(farm_id) IS NOT NULL);
CREATE POLICY mutations_dml_block ON public.server_mutations FOR ALL USING (false) WITH CHECK (false);

-- Trigger to Enforce Absolute Immutability on Financial Ledgers
CREATE OR REPLACE FUNCTION enforce_financial_immutability()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Financial ledger records are fully immutable and append-only. Use compensating reversal entries.' USING ERRCODE = 'P0001';
END;
$$ LANGUAGE plpgsql SET search_path = public, pg_temp;

CREATE TRIGGER trg_financial_immutable
    BEFORE UPDATE OR DELETE ON public.financial_ledgers
    FOR EACH ROW EXECUTE FUNCTION enforce_financial_immutability();

-- BEFORE Trigger for Transactionally Consistent Per-Farm Revision Locking & Mutating Feed
CREATE OR REPLACE FUNCTION record_server_mutation()
RETURNS TRIGGER AS $$
DECLARE
    v_farm_id UUID;
    v_next_rev BIGINT;
    v_entity_id UUID;
BEGIN
    v_farm_id := COALESCE(NEW.farm_id, OLD.farm_id);
    v_entity_id := COALESCE(NEW.global_id, OLD.global_id);

    INSERT INTO public.farm_revisions (farm_id, current_revision)
    VALUES (v_farm_id, 0)
    ON CONFLICT (farm_id) DO NOTHING;

    SELECT current_revision + 1 INTO v_next_rev
    FROM public.farm_revisions
    WHERE farm_id = v_farm_id
    FOR UPDATE;

    UPDATE public.farm_revisions
    SET current_revision = v_next_rev
    WHERE farm_id = v_farm_id;

    IF (TG_OP = 'INSERT') THEN
        NEW.server_revision := v_next_rev;
        INSERT INTO public.server_mutations (farm_id, mutation_revision, entity_type, entity_id, operation_type, payload)
        VALUES (v_farm_id, v_next_rev, TG_TABLE_NAME, v_entity_id, 'INSERT', row_to_json(NEW)::jsonb);
        RETURN NEW;
    ELSIF (TG_OP = 'UPDATE') THEN
        NEW.server_revision := v_next_rev;
        INSERT INTO public.server_mutations (farm_id, mutation_revision, entity_type, entity_id, operation_type, payload)
        VALUES (v_farm_id, v_next_rev, TG_TABLE_NAME, v_entity_id, 'UPDATE', row_to_json(NEW)::jsonb);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO public.server_mutations (farm_id, mutation_revision, entity_type, entity_id, operation_type, payload)
        VALUES (v_farm_id, v_next_rev, TG_TABLE_NAME, v_entity_id, 'DELETE', row_to_json(OLD)::jsonb);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE TRIGGER trg_livestock_mutation
    BEFORE INSERT OR UPDATE OR DELETE ON public.livestock
    FOR EACH ROW EXECUTE FUNCTION record_server_mutation();

CREATE TRIGGER trg_financial_mutation
    BEFORE INSERT ON public.financial_ledgers
    FOR EACH ROW EXECUTE FUNCTION record_server_mutation();

-- 6. Authorized Operations Functions
CREATE OR REPLACE FUNCTION create_livestock(
    p_farm_id UUID,
    p_tag VARCHAR,
    p_species VARCHAR,
    p_breed VARCHAR,
    p_sex VARCHAR DEFAULT 'UNKNOWN'
) RETURNS UUID AS $$
DECLARE
    v_role VARCHAR;
    v_global_id UUID := uuid_generate_v4();
BEGIN
    v_role := auth.get_user_role(p_farm_id);
    IF v_role IS NULL OR v_role NOT IN ('OWNER', 'MANAGER', 'WORKER') THEN
        RAISE EXCEPTION 'Access denied: insufficient privileges to create livestock.' USING ERRCODE = '42501';
    END IF;

    INSERT INTO public.livestock (
        farm_id, global_id, tag, species, breed, sex
    ) VALUES (
        p_farm_id, v_global_id, p_tag, p_species, p_breed, p_sex
    );

    RETURN v_global_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION update_livestock_revision(
    p_farm_id UUID,
    p_global_id UUID,
    p_expected_revision BIGINT,
    p_new_breed VARCHAR
) RETURNS BIGINT AS $$
DECLARE
    v_role VARCHAR;
    v_current_rev BIGINT;
BEGIN
    v_role := auth.get_user_role(p_farm_id);
    IF v_role IS NULL OR v_role NOT IN ('OWNER', 'MANAGER', 'WORKER') THEN
        RAISE EXCEPTION 'Access denied: insufficient privileges to update livestock.' USING ERRCODE = '42501';
    END IF;

    SELECT server_revision INTO v_current_rev
    FROM public.livestock
    WHERE farm_id = p_farm_id AND global_id = p_global_id
    FOR UPDATE;

    IF v_current_rev IS NULL THEN
        RAISE EXCEPTION 'Entity not found.' USING ERRCODE = 'P0002';
    END IF;

    IF v_current_rev <> p_expected_revision THEN
        RAISE EXCEPTION 'Concurrency conflict: expected revision % but found %.', p_expected_revision, v_current_rev USING ERRCODE = '40001';
    END IF;

    UPDATE public.livestock
    SET breed = p_new_breed, updated_at = NOW()
    WHERE farm_id = p_farm_id AND global_id = p_global_id;

    SELECT server_revision INTO v_current_rev FROM public.livestock WHERE farm_id = p_farm_id AND global_id = p_global_id;
    RETURN v_current_rev;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION insert_financial_ledger(
    p_farm_id UUID,
    p_entry_type VARCHAR,
    p_category VARCHAR,
    p_amount NUMERIC,
    p_entry_date DATE,
    p_notes TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_role VARCHAR;
    v_global_id UUID := uuid_generate_v4();
    v_user_id UUID;
BEGIN
    v_role := auth.get_user_role(p_farm_id);
    IF v_role IS NULL OR v_role NOT IN ('OWNER', 'MANAGER') THEN
        RAISE EXCEPTION 'Access denied: only owners and managers can record financial transactions.' USING ERRCODE = '42501';
    END IF;

    v_user_id := auth.current_user_id();

    INSERT INTO public.financial_ledgers (
        farm_id, global_id, entry_type, category, amount, entry_date, notes, created_by
    ) VALUES (
        p_farm_id, v_global_id, p_entry_type, p_category, p_amount, p_entry_date, p_notes, v_user_id
    );

    RETURN v_global_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION reverse_financial_ledger(
    p_farm_id UUID,
    p_target_id UUID,
    p_notes TEXT
) RETURNS UUID AS $$
DECLARE
    v_role VARCHAR;
    v_target financial_ledgers%ROWTYPE;
    v_reversal_id UUID := uuid_generate_v4();
    v_user_id UUID;
BEGIN
    v_role := auth.get_user_role(p_farm_id);
    IF v_role IS NULL OR v_role NOT IN ('OWNER', 'MANAGER') THEN
        RAISE EXCEPTION 'Access denied: financial reversals restricted to owners and managers.' USING ERRCODE = '42501';
    END IF;

    SELECT * INTO v_target
    FROM public.financial_ledgers
    WHERE farm_id = p_farm_id AND global_id = p_target_id;

    IF v_target.global_id IS NULL THEN
        RAISE EXCEPTION 'Target financial record not found.' USING ERRCODE = 'P0002';
    END IF;

    IF v_target.reversal_reference_id IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot reverse a reversal transaction.' USING ERRCODE = 'P0004';
    END IF;

    v_user_id := auth.current_user_id();

    -- Pure Append-Only Reversal. Unique index enforces single reversal constraint.
    INSERT INTO public.financial_ledgers (
        farm_id, global_id, entry_type, category, amount, entry_date, notes, reversal_farm_id, reversal_reference_id, created_by
    ) VALUES (
        p_farm_id,
        v_reversal_id,
        CASE WHEN v_target.entry_type = 'INCOME' THEN 'EXPENSE' ELSE 'INCOME' END,
        'Reversal: ' || v_target.category,
        v_target.amount,
        CURRENT_DATE,
        p_notes,
        p_farm_id,
        v_target.global_id,
        v_user_id
    );

    RETURN v_reversal_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

-- Atomic Idempotency Wrapper Function with Explicit Unsupported Operation Rejection
CREATE OR REPLACE FUNCTION execute_client_operation(
    p_farm_id UUID,
    p_operation_id UUID,
    p_payload JSONB
) RETURNS JSONB AS $$
DECLARE
    v_role VARCHAR;
    v_hash TEXT;
    v_existing client_operations%ROWTYPE;
    v_result JSONB;
BEGIN
    v_role := auth.get_user_role(p_farm_id);
    IF v_role IS NULL THEN
        RAISE EXCEPTION 'Access denied.' USING ERRCODE = '42501';
    END IF;

    v_hash := encode(digest(p_payload::text, 'sha256'), 'hex');

    SELECT * INTO v_existing
    FROM public.client_operations
    WHERE farm_id = p_farm_id AND operation_id = p_operation_id
    FOR UPDATE;

    IF v_existing.operation_id IS NOT NULL THEN
        IF v_existing.payload_hash <> v_hash THEN
            RAISE EXCEPTION 'Idempotency conflict: same operation ID submitted with different payload.' USING ERRCODE = '40901';
        END IF;
        RETURN v_existing.response_payload;
    END IF;

    -- Validate and execute recognized operations, explicitly rejecting unsupported operation types
    IF p_payload ? 'livestock_tag' THEN
        DECLARE
            v_new_id UUID;
        BEGIN
            v_new_id := create_livestock(
                p_farm_id,
                p_payload->>'livestock_tag',
                p_payload->>'species',
                p_payload->>'breed'
            );
            v_result := jsonb_build_object('status', 'success', 'entity_id', v_new_id);
        END;
    ELSIF p_payload ? 'expense_category' THEN
        DECLARE
            v_fin_id UUID;
        BEGIN
            v_fin_id := insert_financial_ledger(
                p_farm_id,
                'EXPENSE',
                p_payload->>'expense_category',
                (p_payload->>'amount')::numeric,
                CURRENT_DATE,
                p_payload->>'notes'
            );
            v_result := jsonb_build_object('status', 'success', 'entity_id', v_fin_id);
        END;
    ELSE
        RAISE EXCEPTION 'Unsupported client operation type. Payload contains no recognized action keys.' USING ERRCODE = '42201';
    END IF;

    -- Store idempotency record atomically
    INSERT INTO public.client_operations (farm_id, operation_id, payload_hash, response_payload)
    VALUES (p_farm_id, p_operation_id, v_hash, v_result);

    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

-- Restricted Application Database Role Setup
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'mfarm_app_user') THEN
        CREATE ROLE mfarm_app_user LOGIN PASSWORD 'env_injected_secure_password';
    END IF;
END $$;

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM mfarm_app_user;
GRANT USAGE ON SCHEMA public, auth TO mfarm_app_user;
GRANT SELECT ON farms, farm_members, farm_revisions, livestock, financial_ledgers, client_operations, server_mutations, milk_production, breeding_records, health_records, vaccinations, inventory, inventory_transactions, audit_logs TO mfarm_app_user;
GRANT SELECT (id, email, full_name, created_at) ON users TO mfarm_app_user;

REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA public, auth FROM PUBLIC, mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.authenticate_session(UUID, UUID) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.current_user_id() TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.get_user_role(UUID) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION create_livestock(UUID, VARCHAR, VARCHAR, VARCHAR, VARCHAR) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION update_livestock_revision(UUID, UUID, BIGINT, VARCHAR) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION insert_financial_ledger(UUID, VARCHAR, VARCHAR, NUMERIC, DATE, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION reverse_financial_ledger(UUID, UUID, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION execute_client_operation(UUID, UUID, JSONB) TO mfarm_app_user;

-- Include Full Parity Tables
CREATE TABLE IF NOT EXISTS public.milk_production (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    livestock_tag VARCHAR(100) NOT NULL,
    litres NUMERIC(10,2) NOT NULL,
    shift VARCHAR(20) DEFAULT 'MORNING',
    recorded_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.breeding_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    livestock_tag VARCHAR(100) NOT NULL,
    sire_tag VARCHAR(100),
    mating_date DATE NOT NULL,
    expected_calving_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'PREGNANT',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.health_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    livestock_tag VARCHAR(100) NOT NULL,
    record_type VARCHAR(50) NOT NULL DEFAULT 'ILLNESS',
    disease_name VARCHAR(255),
    symptoms TEXT,
    diagnosis TEXT,
    treatment TEXT,
    cost NUMERIC(12,2) DEFAULT 0,
    vet_name VARCHAR(255),
    record_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.vaccinations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    livestock_tag VARCHAR(100) NOT NULL,
    vaccine_name VARCHAR(255) NOT NULL,
    scheduled_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'UPCOMING',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    quantity NUMERIC(12,2) NOT NULL DEFAULT 0,
    unit VARCHAR(50) DEFAULT 'KG',
    min_quantity NUMERIC(12,2) DEFAULT 10,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.inventory_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES public.inventory(id) ON DELETE CASCADE,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('IN', 'OUT')),
    quantity NUMERIC(12,2) NOT NULL,
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    farm_id UUID NOT NULL REFERENCES public.farms(id) ON DELETE CASCADE,
    user_email VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Secure User Registration & Auth Helper Functions (SECURITY DEFINER)
CREATE OR REPLACE FUNCTION auth.register_user(
    p_email VARCHAR,
    p_password_hash TEXT,
    p_full_name VARCHAR,
    p_farm_name VARCHAR
)
RETURNS TABLE (
    out_session_token UUID,
    out_farm_id UUID,
    out_email VARCHAR,
    out_full_name VARCHAR
) AS $$
DECLARE
    v_user_id UUID;
    v_farm_id UUID;
    v_session_token UUID;
    v_expires_at TIMESTAMPTZ;
    v_default_farm_name VARCHAR;
BEGIN
    IF EXISTS (SELECT 1 FROM public.users WHERE email = p_email) THEN
        RAISE EXCEPTION 'User with this email already exists.' USING ERRCODE = '23505';
    END IF;

    INSERT INTO public.users (email, full_name, password_hash)
    VALUES (p_email, p_full_name, p_password_hash)
    RETURNING id INTO v_user_id;

    v_default_farm_name := COALESCE(NULLIF(p_farm_name, ''), split_part(p_full_name, ' ', 1) || '''s Farm');
    INSERT INTO public.farms (name, location, currency)
    VALUES (v_default_farm_name, 'Default Location', 'UGX')
    RETURNING id INTO v_farm_id;

    INSERT INTO public.farm_members (farm_id, user_id, role)
    VALUES (v_farm_id, v_user_id, 'OWNER');

    v_session_token := uuid_generate_v4();
    v_expires_at := NOW() + INTERVAL '1 hour';
    INSERT INTO public.active_sessions (session_token, user_id, farm_id, expires_at)
    VALUES (v_session_token, v_user_id, v_farm_id, v_expires_at);

    RETURN QUERY SELECT v_session_token, v_farm_id, p_email, p_full_name;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.get_user_auth_data(p_email VARCHAR)
RETURNS TABLE (
    user_id UUID,
    password_hash TEXT,
    farm_id UUID
) AS $$
DECLARE
    v_user_id UUID;
    v_password_hash TEXT;
    v_farm_id UUID;
BEGIN
    SELECT id, u.password_hash INTO v_user_id, v_password_hash
    FROM public.users u
    WHERE u.email = p_email;

    IF v_user_id IS NULL THEN
        RETURN;
    END IF;

    SELECT fm.farm_id INTO v_farm_id
    FROM public.farm_members fm
    WHERE fm.user_id = v_user_id
    LIMIT 1;

    RETURN QUERY SELECT v_user_id, v_password_hash, v_farm_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.create_session(
    p_user_id UUID,
    p_farm_id UUID
)
RETURNS UUID AS $$
DECLARE
    v_session_token UUID;
BEGIN
    v_session_token := uuid_generate_v4();
    INSERT INTO public.active_sessions (session_token, user_id, farm_id, expires_at)
    VALUES (v_session_token, p_user_id, p_farm_id, NOW() + INTERVAL '1 hour');
    RETURN v_session_token;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.revoke_session(
    p_session_token UUID
)
RETURNS VOID AS $$
BEGIN
    DELETE FROM public.active_sessions WHERE session_token = p_session_token;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.get_user_farms(p_token UUID)
RETURNS TABLE (
    id UUID,
    name VARCHAR,
    location TEXT,
    currency VARCHAR,
    role VARCHAR
) AS $$
DECLARE
    v_user_id UUID;
BEGIN
    SELECT user_id INTO v_user_id
    FROM public.active_sessions
    WHERE session_token = p_token AND expires_at > NOW();

    IF v_user_id IS NULL THEN
        RETURN;
    END IF;

    RETURN QUERY
    SELECT f.id, f.name, f.location, f.currency, fm.role
    FROM public.farms f
    JOIN public.farm_members fm ON f.id = fm.farm_id
    WHERE fm.user_id = v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.add_milk_entry(
    p_farm_id UUID,
    p_tag VARCHAR,
    p_litres NUMERIC,
    p_shift VARCHAR,
    p_date DATE,
    p_notes TEXT
) RETURNS UUID AS $$
DECLARE
    v_id UUID;
BEGIN
    INSERT INTO public.milk_production (farm_id, livestock_tag, litres, shift, recorded_date, notes)
    VALUES (p_farm_id, p_tag, p_litres, COALESCE(p_shift, 'MORNING'), COALESCE(p_date, CURRENT_DATE), p_notes)
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.add_breeding_entry(
    p_farm_id UUID,
    p_tag VARCHAR,
    p_sire_tag VARCHAR,
    p_mating_date DATE,
    p_notes TEXT
) RETURNS UUID AS $$
DECLARE
    v_id UUID;
    v_expected DATE;
BEGIN
    v_expected := p_mating_date + INTERVAL '283 days';
    INSERT INTO public.breeding_records (farm_id, livestock_tag, sire_tag, mating_date, expected_calving_date, notes)
    VALUES (p_farm_id, p_tag, p_sire_tag, p_mating_date, v_expected, p_notes)
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.add_health_record(
    p_farm_id UUID,
    p_tag VARCHAR,
    p_type VARCHAR,
    p_disease VARCHAR,
    p_symptoms TEXT,
    p_diagnosis TEXT,
    p_treatment TEXT,
    p_cost NUMERIC,
    p_vet VARCHAR,
    p_date DATE
) RETURNS UUID AS $$
DECLARE
    v_id UUID;
BEGIN
    INSERT INTO public.health_records (farm_id, livestock_tag, record_type, disease_name, symptoms, diagnosis, treatment, cost, vet_name, record_date)
    VALUES (p_farm_id, p_tag, COALESCE(p_type, 'ILLNESS'), p_disease, p_symptoms, p_diagnosis, p_treatment, COALESCE(p_cost, 0), p_vet, COALESCE(p_date, CURRENT_DATE))
    RETURNING id INTO v_id;

    IF COALESCE(p_cost, 0) > 0 THEN
        PERFORM insert_financial_ledger(p_farm_id, 'EXPENSE', 'Veterinary Treatment', p_cost, COALESCE(p_date, CURRENT_DATE), 'Vet Treatment for Tag: ' || p_tag);
    END IF;

    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.add_vaccination(
    p_farm_id UUID,
    p_tag VARCHAR,
    p_vaccine VARCHAR,
    p_date DATE,
    p_notes TEXT
) RETURNS UUID AS $$
DECLARE
    v_id UUID;
BEGIN
    INSERT INTO public.vaccinations (farm_id, livestock_tag, vaccine_name, scheduled_date, notes)
    VALUES (p_farm_id, p_tag, p_vaccine, p_date, p_notes)
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.add_inventory_item(
    p_farm_id UUID,
    p_name VARCHAR,
    p_category VARCHAR,
    p_qty NUMERIC,
    p_unit VARCHAR,
    p_min NUMERIC
) RETURNS UUID AS $$
DECLARE
    v_id UUID;
BEGIN
    INSERT INTO public.inventory (farm_id, item_name, category, quantity, unit, min_quantity)
    VALUES (p_farm_id, p_name, p_category, p_qty, COALESCE(p_unit, 'KG'), COALESCE(p_min, 10))
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

CREATE OR REPLACE FUNCTION auth.record_stock_transaction(
    p_farm_id UUID,
    p_item_id UUID,
    p_type VARCHAR,
    p_qty NUMERIC,
    p_notes TEXT
) RETURNS VOID AS $$
BEGIN
    INSERT INTO public.inventory_transactions (farm_id, item_id, transaction_type, quantity, notes)
    VALUES (p_farm_id, p_item_id, p_type, p_qty, p_notes);

    IF p_type = 'IN' THEN
        UPDATE public.inventory SET quantity = quantity + p_qty, updated_at = NOW() WHERE id = p_item_id AND farm_id = p_farm_id;
    ELSIF p_type = 'OUT' THEN
        UPDATE public.inventory SET quantity = GREATEST(0, quantity - p_qty), updated_at = NOW() WHERE id = p_item_id AND farm_id = p_farm_id;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

-- Grants
GRANT EXECUTE ON FUNCTION auth.register_user(VARCHAR, TEXT, VARCHAR, VARCHAR) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.get_user_auth_data(VARCHAR) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.create_session(UUID, UUID) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.revoke_session(UUID) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.get_user_farms(UUID) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_milk_entry(UUID, VARCHAR, NUMERIC, VARCHAR, DATE, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_breeding_entry(UUID, VARCHAR, VARCHAR, DATE, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_health_record(UUID, VARCHAR, VARCHAR, VARCHAR, TEXT, TEXT, TEXT, NUMERIC, VARCHAR, DATE) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_vaccination(UUID, VARCHAR, VARCHAR, DATE, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_inventory_item(UUID, VARCHAR, VARCHAR, NUMERIC, VARCHAR, NUMERIC) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.record_stock_transaction(UUID, UUID, VARCHAR, NUMERIC, TEXT) TO mfarm_app_user;
