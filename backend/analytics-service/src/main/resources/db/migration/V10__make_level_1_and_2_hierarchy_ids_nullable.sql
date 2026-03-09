-- Ensure level_1 and level_2 hierarchy ids are nullable in dimension tables

-- ============================================================
-- DIM LGD LOCATION TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_lgd_location_table
    ALTER COLUMN level_1_lgd_id DROP NOT NULL,
    ALTER COLUMN level_2_lgd_id DROP NOT NULL;

-- ============================================================
-- DIM DEPARTMENT LOCATION TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_department_location_table
    ALTER COLUMN level_1_dept_id DROP NOT NULL,
    ALTER COLUMN level_2_dept_id DROP NOT NULL;

-- ============================================================
-- DIM SCHEME TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN level_1_lgd_id DROP NOT NULL,
    ALTER COLUMN level_2_lgd_id DROP NOT NULL,
    ALTER COLUMN level_1_dept_id DROP NOT NULL,
    ALTER COLUMN level_2_dept_id DROP NOT NULL;
