-- Allow nullable parent department mapping and duplicate scheme IDs
-- across multiple parent departments in dim_scheme_table.

-- 1) parent_department_location_id should be nullable.
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN parent_department_location_id DROP NOT NULL;

-- 2) Drop foreign keys that depend on dim_scheme_table(scheme_id),
--    because scheme_id will no longer be unique/primary.
ALTER TABLE analytics_schema.fact_meter_reading_table
    DROP CONSTRAINT IF EXISTS fact_meter_reading_table_scheme_id_fkey;

ALTER TABLE analytics_schema.fact_water_quantity_table
    DROP CONSTRAINT IF EXISTS fact_water_quantity_table_scheme_id_fkey;

ALTER TABLE analytics_schema.fact_escalation_table
    DROP CONSTRAINT IF EXISTS fact_escalation_table_scheme_id_fkey;

ALTER TABLE analytics_schema.fact_scheme_performance_table
    DROP CONSTRAINT IF EXISTS fact_scheme_performance_table_scheme_id_fkey;

ALTER TABLE analytics_schema.dim_operator_attendance_table
    DROP CONSTRAINT IF EXISTS dim_operator_attendance_table_scheme_id_fkey;

ALTER TABLE analytics_schema.dim_user_scheme_mapping_table
    DROP CONSTRAINT IF EXISTS dim_user_scheme_mapping_table_scheme_id_fkey;

-- 3) Drop the primary key on scheme_id to allow multiple rows
--    per scheme across different parent mappings.
ALTER TABLE analytics_schema.dim_scheme_table
    DROP CONSTRAINT IF EXISTS dim_scheme_table_pkey;

-- Keep lookups on scheme_id efficient after dropping PK.
CREATE INDEX IF NOT EXISTS idx_dim_scheme_scheme_id
    ON analytics_schema.dim_scheme_table(scheme_id);
