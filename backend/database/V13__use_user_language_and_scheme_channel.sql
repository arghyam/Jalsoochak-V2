-- Move language/channel preference storage to tenant schema tables:
-- - user_table.language_id
-- - scheme_master_table.channel
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
            'ALTER TABLE %I.user_table ADD COLUMN IF NOT EXISTS language_id INTEGER',
            r.schema_name
        );
        EXECUTE format(
            'ALTER TABLE %I.scheme_master_table ADD COLUMN IF NOT EXISTS channel INTEGER',
            r.schema_name
        );
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%s_user_language_id ON %I.user_table(language_id)',
            r.schema_name, r.schema_name
        );
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%s_scheme_channel ON %I.scheme_master_table(channel)',
            r.schema_name, r.schema_name
        );
    END LOOP;

    -- 2) Future tenants: patch provisioning function
    IF to_regprocedure('create_tenant_schema(text)') IS NULL THEN
        RAISE NOTICE 'Function create_tenant_schema(text) not found. Skipping function patch.';
        RETURN;
    END IF;

    SELECT pg_get_functiondef('create_tenant_schema(text)'::regprocedure)
      INTO fn_definition;

    patched_definition := fn_definition;

    IF position('language_id' IN patched_definition) = 0 THEN
        patched_definition := replace(
            patched_definition,
            E'            phone_verification_status   BOOLEAN         DEFAULT FALSE,\n            created_by',
            E'            phone_verification_status   BOOLEAN         DEFAULT FALSE,\n            language_id                 INTEGER,\n            created_by'
        );
    END IF;

    IF position('idx_%1$s_user_language_id' IN patched_definition) = 0 THEN
        patched_definition := replace(
            patched_definition,
            E'    EXECUTE format(''CREATE INDEX idx_%1$s_user_tenant       ON %1$I.user_table(tenant_id)'',     schema_name);',
            E'    EXECUTE format(''CREATE INDEX idx_%1$s_user_tenant       ON %1$I.user_table(tenant_id)'',     schema_name);\n    EXECUTE format(''CREATE INDEX idx_%1$s_user_language_id  ON %1$I.user_table(language_id)'',   schema_name);'
        );
    END IF;

    IF position('scheme_master_table(channel)' IN patched_definition) = 0 THEN
        patched_definition := replace(
            patched_definition,
            E'    EXECUTE format(''CREATE INDEX idx_%1$s_scheme_centre_id  ON %1$I.scheme_master_table(centre_scheme_id)'',  schema_name);',
            E'    EXECUTE format(''CREATE INDEX idx_%1$s_scheme_centre_id  ON %1$I.scheme_master_table(centre_scheme_id)'',  schema_name);\n    EXECUTE format(''CREATE INDEX idx_%1$s_scheme_channel    ON %1$I.scheme_master_table(channel)'',          schema_name);'
        );
    END IF;

    IF position('channel             INTEGER' IN patched_definition) = 0 THEN
        patched_definition := replace(
            patched_definition,
            E'            operating_status    INTEGER             NOT NULL,\n            created_at',
            E'            operating_status    INTEGER             NOT NULL,\n            channel             INTEGER,\n            created_at'
        );
    END IF;

    IF patched_definition <> fn_definition THEN
        EXECUTE patched_definition;
    END IF;
END $$;

