-- V16: Create admin_user_token_table for opaque invite and password-reset tokens.
-- The tenant_admin_user_master_table already has a uuid column (from V1).
-- This migration only adds the token table.

CREATE TABLE common_schema.admin_user_token_table (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,   -- SHA-256 hex of raw token
    token_type  VARCHAR(20)  NOT NULL,   -- 'INVITE' | 'RESET'
    metadata    JSONB,                   -- role, tenantCode, tenantName (INVITE only)
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,             -- set when token is consumed (one-time use)
    deleted_at  TIMESTAMPTZ,             -- set when admin-revoked
    deleted_by  INTEGER,                 -- FK to tenant_admin_user_master_table(id)
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  INTEGER,                 -- FK to tenant_admin_user_master_table(id)

    CONSTRAINT chk_aut_token_type
        CHECK (token_type IN ('INVITE', 'RESET')),
    CONSTRAINT chk_aut_metadata
        CHECK (
            (token_type = 'INVITE' AND metadata IS NOT NULL) OR
            (token_type = 'RESET'  AND metadata IS NULL)
        ),
    CONSTRAINT fk_aut_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_aut_created_by
        FOREIGN KEY (created_by)
        REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- Only one ACTIVE token per email per type (active = not used, not deleted).
-- Emails are always stored lowercase (normalised at application layer),
-- so a plain index is sufficient and more efficient than a functional one.
CREATE UNIQUE INDEX uq_active_admin_token
    ON common_schema.admin_user_token_table(email, token_type)
    WHERE used_at IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX idx_admin_token_hash
    ON common_schema.admin_user_token_table(token_hash);

CREATE INDEX idx_admin_token_email
    ON common_schema.admin_user_token_table(email);
