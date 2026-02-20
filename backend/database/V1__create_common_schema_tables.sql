-- ============================================================
-- Migration: V1 - Create common_schema and all tables
-- Schema: common_schema
-- ============================================================

-- Create schema
CREATE SCHEMA IF NOT EXISTS common_schema;

SET search_path TO common_schema;

-- ============================================================
-- 1. tenant_admin_user_master_table
--    Created first so that every other table can reference it.
--    tenant_id is loosely coupled (no formal FK) to break circular dependency.
--    admin_level FK is added later via ALTER TABLE.
-- ============================================================

CREATE TABLE common_schema.tenant_admin_user_master_table (
    id                          SERIAL          PRIMARY KEY,
    uuid                        VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    tenant_id                   INTEGER         NOT NULL DEFAULT 0, -- loosely coupled to tenant_master_table (no formal FK) -- 0 for seeded/system users
    email                       VARCHAR(255)    NOT NULL,
    phone_number                TEXT            NOT NULL,   -- stored encrypted
    admin_level                 INTEGER,                    -- FK to user_type_master_table (added later)
    password                    TEXT            NOT NULL,   -- stored encrypted
    email_verification_status   BOOLEAN         NOT NULL DEFAULT FALSE,
    phone_verification_status   BOOLEAN         NOT NULL DEFAULT FALSE,
    status                      INTEGER         NOT NULL,
    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by                  INTEGER,
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by                  INTEGER,
    deleted_at                  TIMESTAMP,
    deleted_by                  INTEGER,

    CONSTRAINT fk_admin_user_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_admin_user_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_admin_user_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 2. user_type_master_table
-- ============================================================

CREATE TABLE common_schema.user_type_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    c_name          VARCHAR(255)    NOT NULL UNIQUE,  -- e.g. OPERATOR, SUPER_ADMIN
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_by      INTEGER,
    deleted_at      TIMESTAMP,
    deleted_by      INTEGER,

    CONSTRAINT fk_user_type_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_user_type_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_user_type_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 3. tenant_master_table
-- ============================================================

CREATE TABLE common_schema.tenant_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    state_code      VARCHAR(10)     NOT NULL UNIQUE,  -- e.g. MH, UP, MP
    lgd_code        INTEGER         NOT NULL UNIQUE,
    title           VARCHAR(255)    NOT NULL, -- GOA
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    onboarded_at    TIMESTAMP,
    status          INTEGER         NOT NULL, -- 1: ACTIVE, 0: INACTIVE
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      INTEGER,
    deleted_at      TIMESTAMP,
    deleted_by      INTEGER,

    CONSTRAINT fk_tenant_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_tenant_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_tenant_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 4. tenant_config_master_table
-- ============================================================

CREATE TABLE common_schema.tenant_config_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    tenant_id       INTEGER         NOT NULL,
    config_key      TEXT,                              -- e.g. language_1
    config_value    TEXT,                              -- e.g. Hindi
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      INTEGER,
    deleted_at      TIMESTAMP,
    deleted_by      INTEGER,

    CONSTRAINT fk_tenant_config_tenant_id
        FOREIGN KEY (tenant_id) REFERENCES common_schema.tenant_master_table(id),
    CONSTRAINT fk_tenant_config_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_tenant_config_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_tenant_config_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 5. channel_master_table
-- ============================================================

CREATE TABLE common_schema.channel_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    title           VARCHAR(255)    NOT NULL UNIQUE,  -- e.g. whatsapp, sms, email
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_by      INTEGER,
    deleted_at      TIMESTAMP,
    deleted_by      INTEGER,

    CONSTRAINT fk_channel_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_channel_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_channel_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);


-- ============================================================
-- Deferred FK constraints on tenant_admin_user_master_table
-- ============================================================

-- tenant_id is intentionally NOT a formal FK to tenant_master_table
-- to break the circular dependency. Enforced at the application level.
-- (0 = system/super-admin user with no specific tenant)

-- admin_level -> user_type_master_table
ALTER TABLE common_schema.tenant_admin_user_master_table
    ADD CONSTRAINT fk_admin_user_admin_level
    FOREIGN KEY (admin_level) REFERENCES common_schema.user_type_master_table(id);

-- ============================================================
-- Indexes for frequently queried columns
-- ============================================================

CREATE INDEX idx_tenant_master_state_code     ON common_schema.tenant_master_table(state_code);
CREATE INDEX idx_tenant_master_lgd_code       ON common_schema.tenant_master_table(lgd_code);
CREATE INDEX idx_tenant_master_status         ON common_schema.tenant_master_table(status);

CREATE INDEX idx_admin_user_tenant_id         ON common_schema.tenant_admin_user_master_table(tenant_id);
CREATE INDEX idx_admin_user_status            ON common_schema.tenant_admin_user_master_table(status);

CREATE INDEX idx_tenant_config_tenant_id      ON common_schema.tenant_config_master_table(tenant_id);
