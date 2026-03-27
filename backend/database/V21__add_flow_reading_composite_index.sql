-- Adds a composite index on flow_reading_table(scheme_id, created_by, reading_date)
-- to cover the correlated subquery in NudgeRepository.streamUsersWithMissedDays
-- that fetches last_confirmed_reading per operator/scheme.
-- Applied to all existing tenant schemas; the tenant schema creation function
-- should also be updated for new tenants (see V2__create_tenant_schema_function.sql).

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT nspname AS schema_name
        FROM pg_namespace
        WHERE nspname LIKE 'tenant\_%' ESCAPE '\'
    LOOP
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%1$s_flow_scheme_creator_date '
            'ON %1$I.flow_reading_table(scheme_id, created_by, reading_date DESC)',
            r.schema_name
        );
    END LOOP;
END $$;
