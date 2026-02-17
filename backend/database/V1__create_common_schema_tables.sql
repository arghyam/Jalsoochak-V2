-- ============================================================
-- Migration: V1 - Create common_schema and all tables
-- Schema: common_schema
-- ============================================================

-- Create schema
CREATE SCHEMA IF NOT EXISTS common_schema;

SET search_path TO common_schema;

-- ============================================================
-- ENUM Types
-- ============================================================

CREATE TYPE common_schema.tenant_status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TYPE common_schema.admin_level AS ENUM ('SUPER_ADMIN', 'STATE_ADMIN');

-- ============================================================
-- 1. tenant_admin_user_master_table
--    (tenant_id = 0 means the user does not belong to any tenant)
-- ============================================================

CREATE TABLE common_schema.tenant_admin_user_master_table (
    id                          SERIAL          PRIMARY KEY,
    uuid                        VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    tenant_id                   INTEGER         NOT NULL DEFAULT 0,
    email                       VARCHAR(255)    NOT NULL,
    phone_number                TEXT            NOT NULL,   -- stored encrypted
    admin_level                 common_schema.admin_level NOT NULL,
    password                    TEXT            NOT NULL,   -- stored encrypted
    email_verification_status   BOOLEAN         NOT NULL DEFAULT FALSE,
    phone_verification_status   BOOLEAN         NOT NULL DEFAULT FALSE,
    status                      common_schema.tenant_status NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by                  INTEGER         DEFAULT 0,  -- 0 for seeded/system users
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by                  INTEGER         DEFAULT 0   -- 0 for seeded/system users
);

-- ============================================================
-- 2. tenant_master_table
-- ============================================================

CREATE TABLE common_schema.tenant_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    state_code      VARCHAR(10)     NOT NULL UNIQUE,        -- e.g. MH, UP, MP
    lgd_code        INTEGER         NOT NULL UNIQUE,
    name            VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER , 
    onboarded_at    TIMESTAMP,
    status          common_schema.tenant_status NOT NULL DEFAULT 'ACTIVE',
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      INTEGER
);

-- FK: updated_by -> tenant_admin_user_master_table
ALTER TABLE common_schema.tenant_master_table
    ADD CONSTRAINT fk_tenant_updated_by
    FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id);

-- ============================================================
-- 3. lgd_location_type_master_table
-- ============================================================

CREATE TABLE common_schema.lgd_location_type_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    level           SMALLINT        NOT NULL UNIQUE, -- 1, 2, 3, 4, 5
    c_name          VARCHAR(255)    NOT NULL UNIQUE,        -- level name
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_by      INTEGER,

    CONSTRAINT fk_lgd_location_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_lgd_location_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 4. department_location_type_master_table
-- ============================================================

CREATE TABLE common_schema.department_location_type_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    level           SMALLINT        NOT NULL UNIQUE, -- 1, 2, 3, 4, 5
    c_name          VARCHAR(255)    NOT NULL UNIQUE,        -- level name
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_by      INTEGER,

    CONSTRAINT fk_dept_location_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_dept_location_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 5. tenant_config_master_table
-- ============================================================

CREATE TABLE common_schema.tenant_config_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    tenant_id       INTEGER         NOT NULL,
    config_key      TEXT            NOT NULL,                                    -- e.g. language_1
    config_value    TEXT            NOT NULL,                                    -- e.g. Hindi
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      INTEGER,

    CONSTRAINT fk_tenant_config_tenant_id
        FOREIGN KEY (tenant_id) REFERENCES common_schema.tenant_master_table(id),
    CONSTRAINT fk_tenant_config_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_tenant_config_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 6. user_type_master_table
-- ============================================================

CREATE TABLE common_schema.user_type_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    c_name          VARCHAR(255)    NOT NULL UNIQUE,
    title           VARCHAR(255)    NOT NULL, -- practically c_name is same as title
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_by      INTEGER,

    CONSTRAINT fk_user_type_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_user_type_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- 7. user_master_table
-- ============================================================

CREATE TABLE common_schema.channel_master_table (
    id              SERIAL          PRIMARY KEY,
    uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    name            VARCHAR(255)    NOT NULL UNIQUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      INTEGER,
    updated_by      INTEGER,

    CONSTRAINT fk_channel_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_channel_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

-- ============================================================
-- Indexes for frequently queried columns
-- ============================================================

CREATE INDEX idx_tenant_master_state_code     ON common_schema.tenant_master_table(state_code);
CREATE INDEX idx_tenant_master_lgd_code       ON common_schema.tenant_master_table(lgd_code);
CREATE INDEX idx_tenant_master_status         ON common_schema.tenant_master_table(status);

CREATE INDEX idx_admin_user_tenant_id         ON common_schema.tenant_admin_user_master_table(tenant_id);
-- CREATE INDEX idx_admin_user_email             ON common_schema.tenant_admin_user_master_table(email);
CREATE INDEX idx_admin_user_status            ON common_schema.tenant_admin_user_master_table(status);

CREATE INDEX idx_tenant_config_tenant_id      ON common_schema.tenant_config_master_table(tenant_id);
-- CREATE INDEX idx_tenant_config_key            ON common_schema.tenant_config_master_table(config_key);
