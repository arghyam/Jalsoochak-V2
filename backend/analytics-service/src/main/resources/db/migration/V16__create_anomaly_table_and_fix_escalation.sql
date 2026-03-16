-- 1. Make scheme_id nullable in fact_escalation_table (officer-level rows have no single scheme)
ALTER TABLE analytics_schema.fact_escalation_table
    ALTER COLUMN scheme_id DROP NOT NULL;

-- 2. Create anomaly_table in analytics_schema
CREATE TABLE IF NOT EXISTS analytics_schema.anomaly_table (
    id                        SERIAL PRIMARY KEY,
    uuid                      TEXT NOT NULL UNIQUE,
    type                      INTEGER NOT NULL,
    user_id                   INTEGER,
    scheme_id                 INTEGER,
    tenant_id                 INTEGER,
    ai_reading                NUMERIC,
    ai_confidence_percentage  NUMERIC,
    overridden_reading        NUMERIC,
    retries                   INTEGER,
    previous_reading          NUMERIC,
    previous_reading_date     DATE,
    consecutive_days_missed   INTEGER,
    reason                    TEXT,
    status                    INTEGER NOT NULL,
    remarks                   TEXT,
    correlation_id            TEXT,
    resolved_by               INTEGER,
    resolved_at               TIMESTAMPTZ,
    deleted_at                TIMESTAMPTZ,
    deleted_by                INTEGER,
    created_at                TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_anomaly_tenant ON analytics_schema.anomaly_table(tenant_id);
CREATE INDEX IF NOT EXISTS idx_anomaly_scheme ON analytics_schema.anomaly_table(scheme_id);
CREATE INDEX IF NOT EXISTS idx_anomaly_type   ON analytics_schema.anomaly_table(type);
CREATE INDEX IF NOT EXISTS idx_anomaly_corr   ON analytics_schema.anomaly_table(correlation_id);
