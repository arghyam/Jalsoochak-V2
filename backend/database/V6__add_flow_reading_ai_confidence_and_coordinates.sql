-- ============================================================
-- Migration: V6 - Add AI confidence and coordinates to flow_reading_table
-- Applies to all existing tenant schemas.
-- ============================================================

DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    FOR tenant_schema IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant\_%' ESCAPE '\'
    LOOP
        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.flow_reading_table
             ADD COLUMN IF NOT EXISTS ai_confidence_percentage NUMERIC',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.flow_reading_table
             ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.flow_reading_table
             ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION',
            tenant_schema
        );
    END LOOP;
END $$;
