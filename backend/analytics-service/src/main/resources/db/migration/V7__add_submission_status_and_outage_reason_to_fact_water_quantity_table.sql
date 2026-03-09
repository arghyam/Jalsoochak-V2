-- Add submission status and outage reason for water quantity records

-- ============================================================
-- FACT WATER QUANTITY TABLE
-- ============================================================
ALTER TABLE analytics_schema.fact_water_quantity_table
    ADD COLUMN IF NOT EXISTS submission_status INT;

ALTER TABLE analytics_schema.fact_water_quantity_table
    ADD COLUMN IF NOT EXISTS outage_reason INT;
