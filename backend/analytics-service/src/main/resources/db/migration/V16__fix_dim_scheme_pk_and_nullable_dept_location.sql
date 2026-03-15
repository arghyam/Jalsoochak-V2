-- ============================================================
-- Fix dim_scheme_table to support one scheme mapped to multiple villages:
--
--   1. Drop FK constraints on scheme_id from all referencing tables.
--      PostgreSQL only allows FKs to reference columns with a PRIMARY KEY
--      or UNIQUE constraint. Since scheme_id will no longer be unique
--      (a scheme can appear once per village), these constraints are invalid.
--      Referential integrity is maintained by the ETL/sync layer.
--
--   2. Drop the current PRIMARY KEY on scheme_id.
--
--   3. Add a composite PRIMARY KEY on (scheme_id, parent_lgd_location_id)
--      to uniquely identify each scheme-village row.
--
--   4. Make parent_department_location_id nullable — not every tenant
--      has a department location data configured.
-- ============================================================

-- Step 1: Drop FK constraints referencing dim_scheme_table(scheme_id)
ALTER TABLE analytics_schema.fact_meter_reading_table
    DROP CONSTRAINT IF EXISTS fact_meter_reading_table_scheme_id_fkey;

ALTER TABLE analytics_schema.fact_water_quantity_table
    DROP CONSTRAINT IF EXISTS fact_water_quantity_table_scheme_id_fkey;

ALTER TABLE analytics_schema.fact_escalation_table
    DROP CONSTRAINT IF EXISTS fact_escalation_table_scheme_id_fkey;

ALTER TABLE analytics_schema.fact_scheme_performance_table
    DROP CONSTRAINT IF EXISTS fact_scheme_performance_table_scheme_id_fkey;

ALTER TABLE analytics_schema.dim_user_scheme_mapping_table
    DROP CONSTRAINT IF EXISTS dim_user_scheme_mapping_table_scheme_id_fkey;

ALTER TABLE analytics_schema.dim_operator_attendance_table
    DROP CONSTRAINT IF EXISTS dim_operator_attendance_table_scheme_id_fkey;

-- Step 2: Drop the primary key on scheme_id
ALTER TABLE analytics_schema.dim_scheme_table
    DROP CONSTRAINT IF EXISTS dim_scheme_table_pkey;

-- Step 3: Add composite primary key on (scheme_id, parent_lgd_location_id)
ALTER TABLE analytics_schema.dim_scheme_table
    ADD CONSTRAINT dim_scheme_table_pkey PRIMARY KEY (scheme_id, parent_lgd_location_id);

-- Step 4: Make parent_department_location_id nullable
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN parent_department_location_id DROP NOT NULL;
