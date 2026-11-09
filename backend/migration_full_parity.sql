-- =====================================================================
-- Migration: Full Platform Parity (Milk, Breeding, Health, Inventory, Audit)
-- =====================================================================

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

-- SECURITY DEFINER Functions
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
GRANT SELECT ON public.milk_production, public.breeding_records, public.health_records, public.vaccinations, public.inventory, public.inventory_transactions, public.audit_logs TO mfarm_app_user;

GRANT EXECUTE ON FUNCTION auth.add_milk_entry(UUID, VARCHAR, NUMERIC, VARCHAR, DATE, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_breeding_entry(UUID, VARCHAR, VARCHAR, DATE, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_health_record(UUID, VARCHAR, VARCHAR, VARCHAR, TEXT, TEXT, TEXT, NUMERIC, VARCHAR, DATE) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_vaccination(UUID, VARCHAR, VARCHAR, DATE, TEXT) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.add_inventory_item(UUID, VARCHAR, VARCHAR, NUMERIC, VARCHAR, NUMERIC) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.record_stock_transaction(UUID, UUID, VARCHAR, NUMERIC, TEXT) TO mfarm_app_user;
