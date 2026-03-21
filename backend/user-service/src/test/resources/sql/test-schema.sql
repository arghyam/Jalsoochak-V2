-- Minimal schema for integration tests.
-- Mirrors the production DDL from backend/database/ migrations.

CREATE SCHEMA IF NOT EXISTS common_schema;
CREATE SCHEMA IF NOT EXISTS tenant_mp;

-- ── Role / user-type lookup ────────────────────────────────────────────────

CREATE TABLE common_schema.user_type_master_table (
    id   SERIAL PRIMARY KEY,
    c_name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO common_schema.user_type_master_table (id, c_name)
VALUES (1, 'SUPER_USER'), (2, 'STATE_ADMIN');

-- ── Tenant master ──────────────────────────────────────────────────────────

CREATE TABLE common_schema.tenant_master_table (
    id         SERIAL PRIMARY KEY,
    state_code VARCHAR(10)  NOT NULL UNIQUE,
    title      VARCHAR(200) NOT NULL,
    status     INTEGER      NOT NULL DEFAULT 3
);

INSERT INTO common_schema.tenant_master_table (id, state_code, title, status)
VALUES (1, 'MP', 'Madhya Pradesh', 3);

-- ── System users (SUPER_USER / STATE_ADMIN) ────────────────────────────────

CREATE TABLE common_schema.tenant_admin_user_master_table (
    id               BIGSERIAL    PRIMARY KEY,
    uuid             VARCHAR(36)  UNIQUE,
    email            VARCHAR(255) NOT NULL UNIQUE,
    phone_number     VARCHAR(20)  NOT NULL,
    tenant_id        INTEGER      NOT NULL DEFAULT 0,
    admin_level      INTEGER      REFERENCES common_schema.user_type_master_table(id),
    password         TEXT         NOT NULL DEFAULT 'KEYCLOAK_MANAGED',
    status           INTEGER      NOT NULL DEFAULT 1,
    created_by       INTEGER,
    updated_by       INTEGER,
    deleted_by       INTEGER,
    deleted_at       TIMESTAMP,
    created_at       TIMESTAMP    DEFAULT NOW(),
    updated_at       TIMESTAMP    DEFAULT NOW()
);

CREATE INDEX idx_admin_user_uuid
    ON common_schema.tenant_admin_user_master_table(uuid);

-- ── Admin token table (V16) ────────────────────────────────────────────────

CREATE TABLE common_schema.admin_user_token_table (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    token_type  VARCHAR(20)  NOT NULL,
    metadata    JSONB,
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP,
    deleted_at  TIMESTAMP,
    deleted_by  INTEGER,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  INTEGER,

    CONSTRAINT fk_aut_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_aut_created_by
        FOREIGN KEY (created_by)
        REFERENCES common_schema.tenant_admin_user_master_table(id)
);

CREATE UNIQUE INDEX uq_active_admin_token
    ON common_schema.admin_user_token_table(email, token_type)
    WHERE used_at IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX idx_admin_token_hash
    ON common_schema.admin_user_token_table(token_hash);

CREATE INDEX idx_admin_token_email
    ON common_schema.admin_user_token_table(email);

-- ── Tenant-scoped user table (one example: tenant_mp) ─────────────────────

CREATE TABLE tenant_mp.user_table (
    id                        BIGSERIAL PRIMARY KEY,
    uuid                      VARCHAR(36),
    tenant_id                 INTEGER   NOT NULL,
    user_type                 INTEGER,
    title                     VARCHAR(255),
    email                     VARCHAR(255),
    phone_number              VARCHAR(20),
    password                  TEXT,
    status                    INTEGER,
    email_verification_status BOOLEAN,
    phone_verification_status BOOLEAN,
    created_by                BIGINT,
    created_at                TIMESTAMP DEFAULT NOW(),
    updated_by                BIGINT,
    updated_at                TIMESTAMP DEFAULT NOW()
);