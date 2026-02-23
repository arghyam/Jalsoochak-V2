-- Update create_tenant_schema() so newly created tenant schemas include
-- flow_reading_table.meter_change_reason.
DO $$
DECLARE
    fn_definition TEXT;
    patched_definition TEXT;
BEGIN
    IF to_regprocedure('create_tenant_schema(text)') IS NULL THEN
        RAISE NOTICE 'Function create_tenant_schema(text) not found. Skipping function patch.';
        RETURN;
    END IF;

    SELECT pg_get_functiondef('create_tenant_schema(text)'::regprocedure)
      INTO fn_definition;

    IF position('meter_change_reason' IN fn_definition) > 0 THEN
        RAISE NOTICE 'create_tenant_schema(text) already includes meter_change_reason. Skipping.';
        RETURN;
    END IF;

    patched_definition := replace(
        fn_definition,
        E'            channel             INTEGER,\n            image_url',
        E'            channel             INTEGER,\n            meter_change_reason TEXT,\n            image_url'
    );

    IF patched_definition = fn_definition THEN
        RAISE EXCEPTION 'Unable to patch create_tenant_schema(text): flow_reading_table block not found.';
    END IF;

    EXECUTE patched_definition;
END $$;

