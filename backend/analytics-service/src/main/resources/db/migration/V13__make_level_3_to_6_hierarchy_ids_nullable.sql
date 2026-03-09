-- Ensure level_3 to level_6 hierarchy ids are nullable in dimension tables

-- ============================================================
-- DIM LGD LOCATION TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_lgd_location_table
    ALTER COLUMN level_3_lgd_id DROP NOT NULL,
    ALTER COLUMN level_4_lgd_id DROP NOT NULL,
    ALTER COLUMN level_5_lgd_id DROP NOT NULL,
    ALTER COLUMN level_6_lgd_id DROP NOT NULL;

-- ============================================================
-- DIM DEPARTMENT LOCATION TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_department_location_table
    ALTER COLUMN level_3_dept_id DROP NOT NULL,
    ALTER COLUMN level_4_dept_id DROP NOT NULL,
    ALTER COLUMN level_5_dept_id DROP NOT NULL,
    ALTER COLUMN level_6_dept_id DROP NOT NULL;

-- ============================================================
-- DIM SCHEME TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN level_3_lgd_id DROP NOT NULL,
    ALTER COLUMN level_4_lgd_id DROP NOT NULL,
    ALTER COLUMN level_5_lgd_id DROP NOT NULL,
    ALTER COLUMN level_6_lgd_id DROP NOT NULL,
    ALTER COLUMN level_3_dept_id DROP NOT NULL,
    ALTER COLUMN level_4_dept_id DROP NOT NULL,
    ALTER COLUMN level_5_dept_id DROP NOT NULL,
    ALTER COLUMN level_6_dept_id DROP NOT NULL;
