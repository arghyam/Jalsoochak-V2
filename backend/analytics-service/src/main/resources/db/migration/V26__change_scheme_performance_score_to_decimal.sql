ALTER TABLE analytics_schema.fact_scheme_performance_table
    ALTER COLUMN performance_score TYPE NUMERIC(3, 1)
    USING performance_score::NUMERIC(3, 1);
