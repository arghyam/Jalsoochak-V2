-- Adds indexes used by paginated list endpoints (schemes, scheme mappings, tenant staff).
-- Safe to run multiple times: uses IF NOT EXISTS.
--
-- Note: this does not modify the tenant schema creation function. It applies to existing tenant schemas.

DO $$
DECLARE
    r RECORD;
    schema_name TEXT;
BEGIN
    FOR r IN
        SELECT state_code
        FROM common_schema.tenant_master_table
    LOOP
        schema_name := 'tenant_' || lower(r.state_code);

        -- Staff list/search/sort
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%1$s_user_title      ON %1$I.user_table(title)', schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%1$s_user_created_at ON %1$I.user_table(created_at)', schema_name);

        -- Schemes list/search/sort
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%1$s_scheme_name      ON %1$I.scheme_master_table(scheme_name)', schema_name);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%1$s_scheme_created_at ON %1$I.scheme_master_table(created_at)', schema_name);

        -- Scheme mappings: subdivision name filter/sort
        IF to_regclass(schema_name || '.department_location_master_table') IS NOT NULL THEN
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%1$s_dept_title       ON %1$I.department_location_master_table(title)', schema_name);
        END IF;
    END LOOP;
END $$;
