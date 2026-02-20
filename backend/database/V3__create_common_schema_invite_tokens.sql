-- ============================================================
-- Migration: V3 - Create invite_tokens in common_schema
-- ============================================================

CREATE TABLE IF NOT EXISTS common_schema.invite_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL,
    token       VARCHAR(255)    NOT NULL UNIQUE,
    expires_at  TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    sender_id   BIGINT          NOT NULL,
    used        BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_invite_tokens_email
    ON common_schema.invite_tokens (LOWER(email));

CREATE INDEX IF NOT EXISTS idx_invite_tokens_sender_id
    ON common_schema.invite_tokens (sender_id);

CREATE INDEX IF NOT EXISTS idx_invite_tokens_expires_at
    ON common_schema.invite_tokens (expires_at);
