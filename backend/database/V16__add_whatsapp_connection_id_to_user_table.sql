-- Add whatsapp_connection_id (Glific contact ID) to user_table.
-- Apply to existing tenant schemas and patch create_tenant_schema() for new tenants.

DO $$
DECLARE
    r RECORD;
    fn_definition TEXT;
    patched_definition TEXT;
BEGIN
    -- 1) Existing tenant schemas
    FOR r IN
        SELECT nspname AS schema_name
        FROM pg_namespace
        WHERE nspname LIKE 'tenant\_%' ESCAPE '\'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.user_table ADD COLUMN IF NOT EXISTS whatsapp_connection_id BIGINT',
            r.schema_name);
    END LOOP;

    -- 2) Future tenants: patch provisioning function
    IF to_regprocedure('create_tenant_schema(text)') IS NULL THEN
        RAISE NOTICE 'Function create_tenant_schema(text) not found. Skipping function patch.';
        RETURN;
    END IF;

    SELECT pg_get_functiondef('create_tenant_schema(text)'::regprocedure)
      INTO fn_definition;

    patched_definition := fn_definition;

    IF position('whatsapp_connection_id' IN patched_definition) = 0 THEN
        patched_definition := replace(patched_definition,
            E'            language_id                 INTEGER,\n            created_by',
            E'            language_id                 INTEGER,\n            whatsapp_connection_id      BIGINT,\n            created_by');
    END IF;

    IF patched_definition <> fn_definition THEN
        EXECUTE patched_definition;
    END IF;
END $$;
