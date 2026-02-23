-- ============================================================
-- Migration: V8 - Store per-user channel preference
-- ============================================================

CREATE TABLE IF NOT EXISTS common_schema.user_channel_preference (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       INTEGER         NOT NULL,
    contact_id      VARCHAR(50)     NOT NULL,
    channel_value   VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_channel_pref UNIQUE (tenant_id, contact_id)
);

CREATE INDEX IF NOT EXISTS idx_user_channel_pref_tenant_contact
    ON common_schema.user_channel_preference (tenant_id, contact_id);
