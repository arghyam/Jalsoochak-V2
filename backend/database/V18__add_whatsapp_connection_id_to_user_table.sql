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
        patched_definition := regexp_replace(
            patched_definition,
            '(language_id\s+INTEGER\s*,\s*)(created_by)',
            E'\\1whatsapp_connection_id      BIGINT,\n            \\2',
            'g'
        );

        IF position('whatsapp_connection_id' IN patched_definition) = 0 THEN
            RAISE EXCEPTION 'V16 patch failed: could not inject whatsapp_connection_id into create_tenant_schema(). '
                'The function body does not contain the expected anchor "language_id INTEGER, ... created_by". '
                'Inspect pg_get_functiondef(''create_tenant_schema(text)''::regprocedure) and update this migration.';
        END IF;
    END IF;

    IF patched_definition <> fn_definition THEN
        EXECUTE patched_definition;
    END IF;
END $$;
