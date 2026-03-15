-- Adds a composite index on flow_reading_table(scheme_id, created_by, reading_date)
-- to cover the correlated subquery in NudgeRepository.streamUsersWithMissedDays
-- that fetches last_confirmed_reading per operator/scheme.
-- Applied to all existing tenant schemas; the tenant schema creation function
-- should also be updated for new tenants (see V2__create_tenant_schema_function.sql).

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
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%1$s_flow_scheme_creator_date '
            'ON %1$I.flow_reading_table(scheme_id, created_by, reading_date DESC)',
            schema_name
        );
    END LOOP;
END $$;
