-- =====================================================================
-- Migration: Add SECURITY DEFINER Auth helper functions
-- =====================================================================

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

GRANT EXECUTE ON FUNCTION auth.register_user(VARCHAR, TEXT, VARCHAR, VARCHAR) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.get_user_auth_data(VARCHAR) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.create_session(UUID, UUID) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.revoke_session(UUID) TO mfarm_app_user;
GRANT EXECUTE ON FUNCTION auth.get_user_farms(UUID) TO mfarm_app_user;
