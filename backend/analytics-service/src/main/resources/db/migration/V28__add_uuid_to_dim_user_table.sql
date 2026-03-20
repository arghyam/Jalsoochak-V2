-- Add uuid column to analytics user dimension

ALTER TABLE analytics_schema.dim_user_table
    ADD COLUMN IF NOT EXISTS uuid UUID;
