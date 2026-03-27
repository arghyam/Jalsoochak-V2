ALTER TABLE analytics_schema.dim_tenant_table
ADD COLUMN IF NOT EXISTS required_lpcd INT;
