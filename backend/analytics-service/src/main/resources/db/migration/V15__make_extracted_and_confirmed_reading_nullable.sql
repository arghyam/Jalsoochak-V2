-- Allow nullable extracted and confirmed readings in fact meter reading table

-- ============================================================
-- FACT METER READING TABLE
-- ============================================================
ALTER TABLE analytics_schema.fact_meter_reading_table
    ALTER COLUMN extracted_reading DROP NOT NULL;

ALTER TABLE analytics_schema.fact_meter_reading_table
    ALTER COLUMN confirmed_reading DROP NOT NULL;
