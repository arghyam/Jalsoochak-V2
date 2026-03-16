-- V22: Patch create_tenant_schema() so new tenants get the composite index
-- on flow_reading_table(scheme_id, created_by, reading_date DESC).
-- Existing tenants already have the index from V21.

DO $$
DECLARE
    fn_definition     TEXT;
    patched_definition TEXT;
    -- Regex anchors on the idx_%1$s_flow_corr line, tolerating any internal whitespace
    anchor_pattern    TEXT := '(EXECUTE format\(''CREATE INDEX idx_%1\$s_flow_corr\s+ON %1\$I\.flow_reading_table\(correlation_id\)'',\s*schema_name\);)';
    new_line          TEXT := E'\n    EXECUTE format(''CREATE INDEX idx_%1$s_flow_scheme_creator_date ON %1$I.flow_reading_table(scheme_id, created_by, reading_date DESC)'', schema_name);';
BEGIN
    IF to_regprocedure('common_schema.create_tenant_schema(text)') IS NULL THEN
        RAISE EXCEPTION 'V22 patch failed: function common_schema.create_tenant_schema(text) not found. '
            'Migration cannot proceed safely — ensure V2 has run before V22.';
    END IF;

    SELECT pg_get_functiondef('common_schema.create_tenant_schema(text)'::regprocedure)
      INTO fn_definition;

    -- Only patch if not already present
    IF position('flow_scheme_creator_date' IN fn_definition) > 0 THEN
        RAISE NOTICE 'Function already contains flow_scheme_creator_date index. Skipping.';
        RETURN;
    END IF;

    patched_definition := regexp_replace(fn_definition, anchor_pattern, '\1' || new_line);

    IF patched_definition = fn_definition THEN
        RAISE EXCEPTION 'V22 patch failed: anchor pattern not found in create_tenant_schema() body. '
            'Migration cannot proceed safely — inspect the function definition manually.';
    END IF;

    EXECUTE patched_definition;
    RAISE NOTICE 'Patched create_tenant_schema() to include flow_scheme_creator_date index.';
END $$;
