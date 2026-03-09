-- Make fact_water_quantity_table.date a foreign key to dim_date_table.full_date

-- Ensure dim_date_table has all dates used by fact_water_quantity_table
INSERT INTO analytics_schema.dim_date_table (
    date_key,
    full_date,
    day,
    month,
    month_name,
    quarter,
    year,
    week,
    is_weekend,
    fiscal_year
)
SELECT
    CAST(TO_CHAR(fwq.date, 'YYYYMMDD') AS INT) AS date_key,
    fwq.date AS full_date,
    EXTRACT(DAY FROM fwq.date)::INT AS day,
    EXTRACT(MONTH FROM fwq.date)::INT AS month,
    TO_CHAR(fwq.date, 'FMMonth') AS month_name,
    EXTRACT(QUARTER FROM fwq.date)::INT AS quarter,
    EXTRACT(YEAR FROM fwq.date)::INT AS year,
    EXTRACT(WEEK FROM fwq.date)::INT AS week,
    (EXTRACT(ISODOW FROM fwq.date) IN (6, 7)) AS is_weekend,
    EXTRACT(YEAR FROM fwq.date)::INT AS fiscal_year
FROM (
    SELECT DISTINCT date
    FROM analytics_schema.fact_water_quantity_table
) fwq
LEFT JOIN analytics_schema.dim_date_table ddt
    ON ddt.full_date = fwq.date
WHERE ddt.full_date IS NULL;

-- full_date must be unique to be referenced by a foreign key
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_dim_date_full_date'
          AND conrelid = 'analytics_schema.dim_date_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.dim_date_table
            ADD CONSTRAINT uk_dim_date_full_date UNIQUE (full_date);
    END IF;
END $$;

-- Add FK from fact date column to date dimension
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_fact_water_quantity_date_dim_date'
          AND conrelid = 'analytics_schema.fact_water_quantity_table'::regclass
    ) THEN
        ALTER TABLE analytics_schema.fact_water_quantity_table
            ADD CONSTRAINT fk_fact_water_quantity_date_dim_date
            FOREIGN KEY (date)
            REFERENCES analytics_schema.dim_date_table (full_date);
    END IF;
END $$;
