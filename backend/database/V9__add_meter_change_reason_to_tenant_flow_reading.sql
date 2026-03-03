-- Add meter_change_reason to flow_reading_table for all existing tenant schemas.
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
            'ALTER TABLE %I.flow_reading_table ADD COLUMN IF NOT EXISTS meter_change_reason TEXT',
            r.schema_name
        );
    END LOOP;
END $$;

