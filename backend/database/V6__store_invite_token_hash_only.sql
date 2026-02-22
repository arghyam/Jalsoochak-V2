-- ============================================================
-- Migration: V6 - Store invite tokens as SHA-256 hashes only
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE common_schema.invite_tokens
    ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64);

UPDATE common_schema.invite_tokens
SET token_hash = encode(digest(token, 'sha256'), 'hex')
WHERE token_hash IS NULL
  AND token IS NOT NULL;

ALTER TABLE common_schema.invite_tokens
    ALTER COLUMN token_hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_invite_tokens_token_hash
    ON common_schema.invite_tokens (token_hash);

ALTER TABLE common_schema.invite_tokens
    ALTER COLUMN token DROP NOT NULL;
