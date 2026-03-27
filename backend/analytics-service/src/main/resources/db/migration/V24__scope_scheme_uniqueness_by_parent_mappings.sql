-- Replace old uniqueness (tenant_id, scheme_id) with uniqueness scoped by
-- parent mappings:
--   (tenant_id, scheme_id, parent_lgd_location_id, parent_department_location_id)
--
-- Why this migration is split into DO blocks:
-- - it stays idempotent (safe when re-run in partially migrated environments)
-- - it can check existence of constraints before dropping/adding
--
-- IMPORTANT:
-- V21 introduced FKs that reference (tenant_id, scheme_id). PostgreSQL requires
-- referenced columns to remain unique. So these dependent FKs must be removed
-- before dropping uq_dim_scheme_tenant_scheme.

DO $$
BEGIN
    -- Remove fact_meter -> dim_scheme composite FK if present.
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_fact_meter_scheme_tenant'
          AND conrelid = 'analytics_schema.fact_meter_reading_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.fact_meter_reading_table
            DROP CONSTRAINT fk_fact_meter_scheme_tenant;
    END IF;

    -- Remove fact_water -> dim_scheme composite FK if present.
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_fact_water_scheme_tenant'
          AND conrelid = 'analytics_schema.fact_water_quantity_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.fact_water_quantity_table
            DROP CONSTRAINT fk_fact_water_scheme_tenant;
    END IF;

    -- Remove fact_scheme_performance -> dim_scheme composite FK if present.
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_fact_perf_scheme_tenant'
          AND conrelid = 'analytics_schema.fact_scheme_performance_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.fact_scheme_performance_table
            DROP CONSTRAINT fk_fact_perf_scheme_tenant;
    END IF;

    -- Remove attendance -> dim_scheme composite FK if present.
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_attendance_scheme_tenant'
          AND conrelid = 'analytics_schema.dim_operator_attendance_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.dim_operator_attendance_table
            DROP CONSTRAINT fk_attendance_scheme_tenant;
    END IF;
END $$;

DO $$
BEGIN
    -- Drop prior uniqueness on (tenant_id, scheme_id) if it exists.
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_dim_scheme_tenant_scheme'
          AND conrelid = 'analytics_schema.dim_scheme_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.dim_scheme_table
            DROP CONSTRAINT uq_dim_scheme_tenant_scheme;
    END IF;

    -- Add new uniqueness scoped by both parent location dimensions.
    -- NULLS NOT DISTINCT ensures NULL parent_department_location_id values
    -- are treated as equal for uniqueness checks (strict dedup behavior).
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_dim_scheme_tenant_scheme_parent_lgd_dept'
          AND conrelid = 'analytics_schema.dim_scheme_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.dim_scheme_table
            ADD CONSTRAINT uq_dim_scheme_tenant_scheme_parent_lgd_dept
            UNIQUE NULLS NOT DISTINCT (
                tenant_id,
                scheme_id,
                parent_lgd_location_id,
                parent_department_location_id
            );
    END IF;
END $$;
