# MFarm Platform — Backend API, PostgreSQL Database & Synchronization Specification

---

## 1. OVERVIEW & DESIGN PRINCIPLES

MFarm connects an offline-first Android native application (`dev.mfarm.com.mfarm`) and a responsive React/TypeScript Web Management Hub through a centralized PostgreSQL multi-tenant backend.

### Key Guarantees
1. **Zero Data Destruction:** Existing SQLite primary keys (`_id INTEGER`) remain intact locally for legacy SQLite queries and foreign keys.
2. **Global Identifier Mapping:** Each SQLite table receives an additive `global_id UUID v4` column to uniquely track records across mobile devices and web browsers.
3. **Multi-Tenant Isolation:** Every server entity and API endpoint enforces strict `farm_id` ownership checks and PostgreSQL Row-Level Security (RLS).
4. **Financial Ledger Immutability:** Income, expense, stock movements, and batch sales cannot be updated or deleted in place. Reversals write compensating offset transactions with full audit log references.
5. **Idempotent Sync Operations:** Sync push batches carry unique `idempotency_key` payloads, making network retries completely safe against duplicate record creation.

---

## 2. POSTGRESQL MULTI-TENANT SCHEMA DRAFT (001_initial_schema.sql)

```sql
-- ---------------------------------------------------------------------------------
-- 1. TENANCY & USERS
-- ---------------------------------------------------------------------------------

CREATE TABLE farms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    location TEXT,
    currency VARCHAR(10) DEFAULT 'UGX',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE farm_members (
    farm_id UUID REFERENCES farms(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) CHECK (role IN ('OWNER', 'MANAGER', 'WORKER')),
    PRIMARY KEY (farm_id, user_id)
);

-- ---------------------------------------------------------------------------------
-- 2. LIVESTOCK & BATCHES
-- ---------------------------------------------------------------------------------

CREATE TABLE livestock (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    local_id BIGINT,
    tag VARCHAR(100) NOT NULL,
    species VARCHAR(100) NOT NULL,
    breed VARCHAR(100),
    dob DATE,
    sex VARCHAR(20) CHECK (sex IN ('MALE', 'FEMALE', 'UNKNOWN')),
    sire_id UUID REFERENCES livestock(id),
    dam_id UUID REFERENCES livestock(id),
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SOLD', 'DECEASED')),
    notes TEXT,
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE livestock_batches (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    batch_name VARCHAR(255) NOT NULL,
    species VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    housing_unit VARCHAR(100),
    arrival_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE vaccinations (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    livestock_id UUID REFERENCES livestock(id),
    batch_id UUID REFERENCES livestock_batches(id),
    vaccine_name VARCHAR(255) NOT NULL,
    due_date DATE NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    completed_date DATE,
    performed_by UUID REFERENCES users(id),
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ---------------------------------------------------------------------------------
-- 3. CROP ENTERPRISES
-- ---------------------------------------------------------------------------------

CREATE TABLE crop_fields (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    field_name VARCHAR(100) NOT NULL,
    area_acres NUMERIC(8, 2) NOT NULL,
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY
);

CREATE TABLE crop_enterprises (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    field_id UUID NOT NULL REFERENCES crop_fields(id),
    crop_type VARCHAR(100) NOT NULL,
    planting_date DATE NOT NULL,
    expected_harvest_date DATE,
    status VARCHAR(50) DEFAULT 'PLANTED',
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE crop_activities (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    crop_enterprise_id UUID NOT NULL REFERENCES crop_enterprises(id),
    activity_type VARCHAR(50) CHECK (activity_type IN ('PLANTING', 'WEEDING', 'FERTILIZING', 'SPRAYING', 'HARVEST')),
    activity_date DATE NOT NULL,
    cost NUMERIC(12, 2) DEFAULT 0.00,
    yield_quantity NUMERIC(12, 2),
    notes TEXT,
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ---------------------------------------------------------------------------------
-- 4. FINANCIAL LEDGERS & REVERSALS (IMMUTABLE)
-- ---------------------------------------------------------------------------------

CREATE TABLE financial_ledgers (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    entry_type VARCHAR(20) CHECK (entry_type IN ('INCOME', 'EXPENSE')),
    category VARCHAR(100) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    entry_date DATE NOT NULL,
    notes TEXT,
    is_reversed BOOLEAN DEFAULT FALSE,
    reversal_reference_id UUID REFERENCES financial_ledgers(id),
    created_by UUID REFERENCES users(id),
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ---------------------------------------------------------------------------------
-- 5. INVENTORY & STOCK MOVEMENTS
-- ---------------------------------------------------------------------------------

CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    quantity NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    unit VARCHAR(50) NOT NULL,
    reorder_threshold NUMERIC(12, 2) DEFAULT 0.00,
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    item_id UUID NOT NULL REFERENCES inventory_items(id),
    transaction_type VARCHAR(50) CHECK (transaction_type IN ('RECEIPT', 'USAGE', 'ADJUSTMENT', 'REVERSAL')),
    quantity NUMERIC(12, 2) NOT NULL,
    reference_id UUID,
    server_revision BIGINT GENERATED ALWAYS AS IDENTITY,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ---------------------------------------------------------------------------------
-- 6. AUDIT LOGS, IDEMPOTENCY PERSISTENCE & TOMBSTONES
-- ---------------------------------------------------------------------------------

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id UUID NOT NULL REFERENCES farms(id),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    changes JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id UUID NOT NULL REFERENCES farms(id),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    response_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '24 hours')
);

CREATE TABLE sync_tombstones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id UUID NOT NULL REFERENCES farms(id),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    deleted_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 3. MIGRATION ROLLBACK PROCEDURES (001_initial_schema.down.sql)

```sql
-- Disaster Recovery Rollback Script
DROP TABLE IF EXISTS sync_tombstones CASCADE;
DROP TABLE IF EXISTS idempotency_keys CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS inventory_transactions CASCADE;
DROP TABLE IF EXISTS inventory_items CASCADE;
DROP TABLE IF EXISTS financial_ledgers CASCADE;
DROP TABLE IF EXISTS crop_activities CASCADE;
DROP TABLE IF EXISTS crop_enterprises CASCADE;
DROP TABLE IF EXISTS crop_fields CASCADE;
DROP TABLE IF EXISTS vaccinations CASCADE;
DROP TABLE IF EXISTS livestock_batches CASCADE;
DROP TABLE IF EXISTS livestock CASCADE;
DROP TABLE IF EXISTS farm_members CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS farms CASCADE;
```

---

## 4. BIDIRECTIONAL SYNCHRONIZATION PROTOCOL

```
  Android Client (Room/SQLite)                   Backend REST API (PostgreSQL)
------------------------------                  -------------------------------
             |                                                 |
             |   1. POST /api/v1/sync/push                     |
             |      Headers: X-Farm-ID, X-Idempotency-Key      |
             |      Body: { mutations: [ ... ] }               |
             |------------------------------------------------>|
             |                                                 | -- Verify Farm Membership & Role
             |                                                 | -- Check idempotency_keys table for replay
             |                                                 | -- Apply Append-Only Ledgers & Server Revisions
             |   2. 200 OK                                     |
             |      { processed: [...], max_revision: REV }    |
             |<------------------------------------------------|
             |                                                 |
             |   3. GET /api/v1/sync/pull                      |
             |      ?since_revision=REV                        |
             |------------------------------------------------>|
             |                                                 | -- Query rows WHERE server_revision > REV
             |   4. 200 OK                                     |
             |      { delta: [...], tombstones: [...] }        |
             |<------------------------------------------------|
```

### Sync Safeguards & Policies
1. **Explicit User Opt-In:** The mobile app displays an explicit confirmation dialog before initial cloud connection. Local SQLite records are never transmitted without user authorization.
2. **Monotonic Change Cursors (`server_revision`):** Eliminates clock-skew issues by assigning monotonic sequence IDs to every server mutation.
3. **Tombstone Retention & Purge Policy:** Deleted records populate `sync_tombstones` and are retained for **90 days** before a background cron purges expired tombstones.
4. **Idempotency Persistence:** The `idempotency_keys` table caches push batch results for 24 hours, preventing duplicate ledger entries during unstable network retries.
