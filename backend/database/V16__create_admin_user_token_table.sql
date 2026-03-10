-- V16: Create admin_user_token_table for opaque invite and password-reset tokens.
-- The tenant_admin_user_master_table already has a uuid column (from V1).
-- This migration only adds the token table.

CREATE TABLE common_schema.admin_user_token_table (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,   -- SHA-256 hex of raw token
    token_type  VARCHAR(20)  NOT NULL,   -- 'INVITE' | 'RESET'
    metadata    JSONB,                   -- role, tenantCode, tenantName (INVITE only)
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP,               -- set when token is consumed (one-time use)
    deleted_at  TIMESTAMP,               -- set when admin-revoked
    deleted_by  INTEGER,                 -- FK to tenant_admin_user_master_table(id)
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  INTEGER,                 -- FK to tenant_admin_user_master_table(id)

    CONSTRAINT fk_aut_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_aut_created_by
        FOREIGN KEY (created_by)
        REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- Only one ACTIVE token per email per type (active = not used, not deleted)
CREATE UNIQUE INDEX uq_active_admin_token
    ON common_schema.admin_user_token_table(email, token_type)
    WHERE used_at IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX idx_admin_token_hash
    ON common_schema.admin_user_token_table(token_hash);

CREATE INDEX idx_admin_token_email
    ON common_schema.admin_user_token_table(email);
