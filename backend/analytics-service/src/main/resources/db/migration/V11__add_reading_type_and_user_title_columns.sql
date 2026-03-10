-- Add reading type for meter readings and title for users

-- ============================================================
-- FACT METER READING TABLE
-- ============================================================
ALTER TABLE analytics_schema.fact_meter_reading_table
    ADD COLUMN IF NOT EXISTS reading_type INT NOT NULL DEFAULT 0;

-- ============================================================
-- DIM USER TABLE
-- ============================================================
ALTER TABLE analytics_schema.dim_user_table
    ADD COLUMN IF NOT EXISTS title VARCHAR(255);
