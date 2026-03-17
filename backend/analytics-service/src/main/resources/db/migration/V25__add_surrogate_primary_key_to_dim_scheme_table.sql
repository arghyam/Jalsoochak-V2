-- Add surrogate key column `id` (INT, auto-increment) and make it the PK.
-- This migration is designed to work for both:
-- - existing tables that already contain rows
-- - fresh environments where the table might be empty

ALTER TABLE analytics_schema.dim_scheme_table
    ADD COLUMN IF NOT EXISTS id INT;

-- Create sequence that will provide auto-increment values for id.
CREATE SEQUENCE IF NOT EXISTS analytics_schema.dim_scheme_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE analytics_schema.dim_scheme_table_id_seq
    OWNED BY analytics_schema.dim_scheme_table.id;

-- Attach sequence as default for future inserts.
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN id SET DEFAULT nextval('analytics_schema.dim_scheme_table_id_seq');

-- Backfill id for pre-existing rows.
UPDATE analytics_schema.dim_scheme_table
SET id = nextval('analytics_schema.dim_scheme_table_id_seq')
WHERE id IS NULL;

-- Re-align sequence so the next generated value is above current MAX(id).
SELECT setval(
    'analytics_schema.dim_scheme_table_id_seq',
    COALESCE((SELECT MAX(id) FROM analytics_schema.dim_scheme_table), 0) + 1,
    false
);

-- id must be mandatory before it can be used as primary key.
ALTER TABLE analytics_schema.dim_scheme_table
    ALTER COLUMN id SET NOT NULL;

DO $$
BEGIN
    -- Add PK on id only when dim_scheme_table currently has no PK with this name.
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'dim_scheme_table_pkey'
          AND conrelid = 'analytics_schema.dim_scheme_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.dim_scheme_table
            ADD CONSTRAINT dim_scheme_table_pkey PRIMARY KEY (id);
    END IF;
END $$;
