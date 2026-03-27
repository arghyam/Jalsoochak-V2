-- Update outage and non-submission reason fields; allow nullable user_id

ALTER TABLE analytics_schema.fact_water_quantity_table
    ALTER COLUMN outage_reason TYPE VARCHAR(255) USING outage_reason::VARCHAR(255),
    ALTER COLUMN outage_reason DROP NOT NULL,
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE analytics_schema.fact_water_quantity_table
    ADD COLUMN IF NOT EXISTS non_submission_reason VARCHAR(255);
