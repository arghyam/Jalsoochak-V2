-- Add status column to dim_scheme_table
-- 1: ACTIVE, 0: INACTIVE
ALTER TABLE analytics_schema.dim_scheme_table
    ADD COLUMN IF NOT EXISTS status INT;
