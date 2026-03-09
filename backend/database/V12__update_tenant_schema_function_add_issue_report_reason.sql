-- Update create_tenant_schema() so newly created tenant schemas include
-- flow_reading_table.issue_report_reason.
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

    IF position('issue_report_reason' IN fn_definition) > 0 THEN
        RAISE NOTICE 'create_tenant_schema(text) already includes issue_report_reason. Skipping.';
        RETURN;
    END IF;

    patched_definition := replace(
        fn_definition,
        E'            meter_change_reason TEXT,\n            image_url',
        E'            meter_change_reason TEXT,\n            issue_report_reason TEXT,\n            image_url'
    );

    IF patched_definition = fn_definition THEN
        patched_definition := replace(
            fn_definition,
            E'            channel             INTEGER,\n            image_url',
            E'            channel             INTEGER,\n            issue_report_reason TEXT,\n            image_url'
        );
    END IF;

    IF patched_definition = fn_definition THEN
        patched_definition := replace(
            fn_definition,
            E'meter_change_reason TEXT,',
            E'meter_change_reason TEXT,\n            issue_report_reason TEXT,'
        );
    END IF;

    IF patched_definition = fn_definition THEN
        patched_definition := replace(
            fn_definition,
            E'image_url           TEXT            DEFAULT '''''',',
            E'issue_report_reason TEXT,\n            image_url           TEXT            DEFAULT '''''','
        );
    END IF;

    IF patched_definition = fn_definition THEN
        RAISE NOTICE 'Unable to patch create_tenant_schema(text): flow_reading_table block not found. Skipping.';
        RETURN;
    END IF;

    EXECUTE patched_definition;
END $$;
