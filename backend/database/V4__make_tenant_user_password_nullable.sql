-- ============================================================
-- Migration: V4 - Make tenant user_table.password nullable
-- ============================================================
-- Keycloak is the source of truth for credentials. Local tenant user records
-- should not require a stored password.

DO $$
DECLARE
    s RECORD;
BEGIN
    FOR s IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant_%'
    LOOP
        EXECUTE format(
            'ALTER TABLE IF EXISTS %I.user_table ALTER COLUMN password DROP NOT NULL',
            s.nspname
        );
    END LOOP;
END$$;
