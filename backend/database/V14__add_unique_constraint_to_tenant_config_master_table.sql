-- ============================================================
-- Migration: V14 - Add unique constraint for tenant config key
-- Schema: common_schema
-- ============================================================

ALTER TABLE common_schema.tenant_config_master_table
    ADD CONSTRAINT uq_tenant_config_tenant_id_config_key
    UNIQUE (tenant_id, config_key);
