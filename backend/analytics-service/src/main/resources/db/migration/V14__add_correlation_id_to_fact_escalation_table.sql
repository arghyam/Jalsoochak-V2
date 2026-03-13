ALTER TABLE analytics_schema.fact_escalation_table
ADD COLUMN IF NOT EXISTS correlation_id VARCHAR;
