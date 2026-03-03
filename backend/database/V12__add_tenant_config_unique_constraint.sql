-- Remove pre-existing duplicate rows (keep the row with the lowest id)
DELETE FROM common_schema.tenant_config_master_table a
    USING common_schema.tenant_config_master_table b
    WHERE a.id > b.id
      AND a.tenant_id = b.tenant_id
      AND a.config_key = b.config_key;

-- Ensure one config row per (tenant, key) — required for safe per-tenant reads and UPSERT writes
ALTER TABLE common_schema.tenant_config_master_table
    ADD CONSTRAINT uq_tenant_config_key UNIQUE (tenant_id, config_key);
