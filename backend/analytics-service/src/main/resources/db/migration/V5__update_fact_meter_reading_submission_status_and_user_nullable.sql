-- Add submission status and allow nullable user for meter readings

-- ============================================================
-- FACT METER READING TABLE
-- ============================================================
ALTER TABLE analytics_schema.fact_meter_reading_table
    ADD COLUMN IF NOT EXISTS submission_status INT;

ALTER TABLE analytics_schema.fact_meter_reading_table
    ALTER COLUMN user_id DROP NOT NULL;
