-- ============================================================
-- Migration: V7 - Ensure new tenant schemas include flow_reading columns
-- ============================================================

DO $$
BEGIN
    -- Preserve the existing implementation by renaming it once.
    IF EXISTS (
        SELECT 1
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE p.proname = 'create_tenant_schema'
          AND n.nspname = 'public'
          AND pg_get_function_identity_arguments(p.oid) = 'schema_name text'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE p.proname = 'create_tenant_schema_v2_base'
          AND n.nspname = 'public'
          AND pg_get_function_identity_arguments(p.oid) = 'schema_name text'
    ) THEN
        ALTER FUNCTION public.create_tenant_schema(text) RENAME TO create_tenant_schema_v2_base;
    END IF;
END $$;

CREATE OR REPLACE FUNCTION public.create_tenant_schema(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $func$
BEGIN
    -- Execute the original provisioning logic first.
    PERFORM public.create_tenant_schema_v2_base(schema_name);

    -- Enforce new flow_reading_table columns for all newly created tenants.
    EXECUTE format(
        'ALTER TABLE IF EXISTS %1$I.flow_reading_table
         ADD COLUMN IF NOT EXISTS ai_confidence_percentage NUMERIC',
        schema_name
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %1$I.flow_reading_table
         ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION',
        schema_name
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %1$I.flow_reading_table
         ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION',
        schema_name
    );
END;
$func$;
