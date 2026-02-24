-- ============================================================
-- Migration: V7 - Store per-user language preference
-- ============================================================

CREATE TABLE IF NOT EXISTS common_schema.user_language_preference (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       INTEGER         NOT NULL,
    contact_id      VARCHAR(50)     NOT NULL,
    language_value  VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_language_pref UNIQUE (tenant_id, contact_id)
);

CREATE INDEX IF NOT EXISTS idx_user_lang_pref_tenant_contact
    ON common_schema.user_language_preference (tenant_id, contact_id);
