-- Remove pre-existing duplicate rows (keep the row with the lowest id)
DELETE FROM common_schema.tenant_config_master_table a
    USING common_schema.tenant_config_master_table b
    WHERE a.id > b.id
      AND a.tenant_id = b.tenant_id
      AND a.config_key = b.config_key;

-- Drop the full unique constraint added in V14 so we can replace it with a
-- partial index that excludes soft-deleted rows.
ALTER TABLE common_schema.tenant_config_master_table
    DROP CONSTRAINT IF EXISTS uq_tenant_config_tenant_id_config_key;

-- Ensure one active config row per (tenant, key); partial index excludes soft-deleted rows
-- so uniqueness is only enforced where deleted_at IS NULL, and a key can be re-inserted
-- after soft-deletion without violating the constraint. NULL config_key values are also
-- excluded from the uniqueness guarantee, matching PostgreSQL NULL-inequality semantics.
-- Note: PostgreSQL only supports partial unique indexes via CREATE UNIQUE INDEX, not ADD CONSTRAINT.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_config_key
    ON common_schema.tenant_config_master_table (tenant_id, config_key)
    WHERE (deleted_at IS NULL);
