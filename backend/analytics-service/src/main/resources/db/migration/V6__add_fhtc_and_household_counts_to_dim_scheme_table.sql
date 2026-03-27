-- ============================================================
-- DIM SCHEME TABLE
-- ============================================================

ALTER TABLE analytics_schema.dim_scheme_table
    ADD COLUMN fhtc_count INT,
    ADD COLUMN planned_fhtc INT,
    ADD COLUMN house_hold_count INT;
