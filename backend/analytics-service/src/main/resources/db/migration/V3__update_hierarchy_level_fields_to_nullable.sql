-- Make hierarchy level fields nullable for analytics dimensions

-- ============================================================
-- DIM LGD LOCATION TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_lgd_location_table
    ALTER COLUMN level_1_lgd_id DROP NOT NULL,
    ALTER COLUMN level_2_lgd_id DROP NOT NULL,
    ALTER COLUMN level_3_lgd_id DROP NOT NULL,
    ALTER COLUMN level_4_lgd_id DROP NOT NULL,
    ALTER COLUMN level_5_lgd_id DROP NOT NULL,
    ALTER COLUMN level_6_lgd_id DROP NOT NULL;

-- ============================================================
-- DIM DEPARTMENT LOCATION TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_department_location_table
    ALTER COLUMN level_1_dept_id DROP NOT NULL,
    ALTER COLUMN level_2_dept_id DROP NOT NULL,
    ALTER COLUMN level_3_dept_id DROP NOT NULL,
    ALTER COLUMN level_4_dept_id DROP NOT NULL,
    ALTER COLUMN level_5_dept_id DROP NOT NULL,
    ALTER COLUMN level_6_dept_id DROP NOT NULL;

-- ============================================================
-- DIM SCHEME TABLE (LGD hierarchy)
-- ============================================================
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN level_1_lgd_id DROP NOT NULL,
    ALTER COLUMN level_2_lgd_id DROP NOT NULL,
    ALTER COLUMN level_3_lgd_id DROP NOT NULL,
    ALTER COLUMN level_4_lgd_id DROP NOT NULL,
    ALTER COLUMN level_5_lgd_id DROP NOT NULL,
    ALTER COLUMN level_6_lgd_id DROP NOT NULL;

-- ============================================================
-- DIM SCHEME TABLE (Department hierarchy)
-- ============================================================
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN level_1_dept_id DROP NOT NULL,
    ALTER COLUMN level_2_dept_id DROP NOT NULL,
    ALTER COLUMN level_3_dept_id DROP NOT NULL,
    ALTER COLUMN level_4_dept_id DROP NOT NULL,
    ALTER COLUMN level_5_dept_id DROP NOT NULL,
    ALTER COLUMN level_6_dept_id DROP NOT NULL;
