-- Drop the FK constraint on scheme_id in fact_escalation_table.
-- Escalation events arrive from tenant-service before scheme dimension data
-- is synced, so enforcing referential integrity here causes FK violations.
-- scheme_id is stored as a plain integer reference instead.
ALTER TABLE analytics_schema.fact_escalation_table
    DROP CONSTRAINT IF EXISTS fact_escalation_table_scheme_id_fkey;
