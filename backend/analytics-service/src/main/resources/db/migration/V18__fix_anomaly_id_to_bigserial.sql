-- Alter anomaly_table.id from INT (SERIAL) to BIGINT to match the Java Long field
-- and avoid 32-bit sequence exhaustion in production.
ALTER TABLE analytics_schema.anomaly_table ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE analytics_schema.anomaly_table_id_seq AS BIGINT;
