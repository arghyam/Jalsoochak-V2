-- Corrective migration for V16 side effects:
-- keep parent_department_location_id nullable, but restore referential integrity
-- using tenant-scoped scheme identity (tenant_id, scheme_id).

-- 1) Enforce uniqueness of scheme_id within a tenant.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_dim_scheme_tenant_scheme'
          AND conrelid = 'analytics_schema.dim_scheme_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.dim_scheme_table
            ADD CONSTRAINT uq_dim_scheme_tenant_scheme UNIQUE (tenant_id, scheme_id);
    END IF;
END $$;

-- 2) Add composite FK constraints back on tables that include tenant_id + scheme_id.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_fact_meter_scheme_tenant'
          AND conrelid = 'analytics_schema.fact_meter_reading_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.fact_meter_reading_table
            ADD CONSTRAINT fk_fact_meter_scheme_tenant
            FOREIGN KEY (tenant_id, scheme_id)
            REFERENCES analytics_schema.dim_scheme_table (tenant_id, scheme_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_fact_water_scheme_tenant'
          AND conrelid = 'analytics_schema.fact_water_quantity_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.fact_water_quantity_table
            ADD CONSTRAINT fk_fact_water_scheme_tenant
            FOREIGN KEY (tenant_id, scheme_id)
            REFERENCES analytics_schema.dim_scheme_table (tenant_id, scheme_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_fact_perf_scheme_tenant'
          AND conrelid = 'analytics_schema.fact_scheme_performance_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.fact_scheme_performance_table
            ADD CONSTRAINT fk_fact_perf_scheme_tenant
            FOREIGN KEY (tenant_id, scheme_id)
            REFERENCES analytics_schema.dim_scheme_table (tenant_id, scheme_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_attendance_scheme_tenant'
          AND conrelid = 'analytics_schema.dim_operator_attendance_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.dim_operator_attendance_table
            ADD CONSTRAINT fk_attendance_scheme_tenant
            FOREIGN KEY (tenant_id, scheme_id)
            REFERENCES analytics_schema.dim_scheme_table (tenant_id, scheme_id);
    END IF;
END $$;

-- Intentionally do not add a scheme FK on fact_escalation_table and
-- dim_user_scheme_mapping_table:
-- - fact_escalation_table is async and may arrive before scheme sync.
-- - dim_user_scheme_mapping_table lacks tenant_id, so a tenant-safe FK cannot be defined.
