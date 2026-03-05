-- =============================================================
-- Minimal test schema for NudgeRepository and TenantConfigService tests.
-- Created without FK constraints for fast, isolated test setup.
-- =============================================================

CREATE SCHEMA IF NOT EXISTS common_schema;

CREATE TABLE common_schema.user_type_master_table (
    id     SERIAL PRIMARY KEY,
    c_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE common_schema.tenant_master_table (
    id         SERIAL PRIMARY KEY,
    uuid       VARCHAR(36) DEFAULT gen_random_uuid()::TEXT,
    state_code VARCHAR(10) NOT NULL UNIQUE,
    lgd_code   INTEGER     NOT NULL DEFAULT 0,
    title      VARCHAR(255) NOT NULL DEFAULT '',
    status     INTEGER      NOT NULL DEFAULT 1,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE common_schema.tenant_config_master_table (
    id          SERIAL PRIMARY KEY,
    uuid        VARCHAR(36) DEFAULT gen_random_uuid()::TEXT,
    tenant_id   INTEGER NOT NULL,
    config_key  TEXT,
    config_value TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed reference data
INSERT INTO common_schema.user_type_master_table (c_name)
VALUES ('OPERATOR'), ('SECTION_OFFICER'), ('DISTRICT_OFFICER');

INSERT INTO common_schema.tenant_master_table (state_code, lgd_code, title, status)
VALUES ('TS', 1, 'Test State', 1);

-- Tenant test schema (mimics tenant_<state_code> schemas)
CREATE SCHEMA IF NOT EXISTS tenant_test;

CREATE TABLE tenant_test.user_table (
    id           SERIAL PRIMARY KEY,
    title        TEXT    NOT NULL,
    phone_number TEXT    NOT NULL,
    user_type    INTEGER NOT NULL,
    language_id  INTEGER DEFAULT 0,
    email        VARCHAR(255) NOT NULL DEFAULT 'noop@test.com',
    tenant_id    INTEGER NOT NULL DEFAULT 1,
    status       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE tenant_test.scheme_master_table (
    id               SERIAL PRIMARY KEY,
    state_scheme_id  VARCHAR(255) NOT NULL DEFAULT 'SCH-001',
    centre_scheme_id VARCHAR(255) NOT NULL DEFAULT '',
    scheme_name      VARCHAR(255) NOT NULL DEFAULT '',
    work_status      INTEGER      NOT NULL DEFAULT 1,
    operating_status INTEGER      NOT NULL DEFAULT 1
);

CREATE TABLE tenant_test.user_scheme_mapping_table (
    id        SERIAL PRIMARY KEY,
    user_id   INTEGER NOT NULL,
    scheme_id INTEGER NOT NULL,
    status    INTEGER NOT NULL
);

CREATE TABLE tenant_test.flow_reading_table (
    id                SERIAL PRIMARY KEY,
    scheme_id         INTEGER NOT NULL,
    reading_date      DATE    NOT NULL,
    reading_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    extracted_reading NUMERIC   NOT NULL DEFAULT 0,
    confirmed_reading NUMERIC   NOT NULL DEFAULT 0,
    correlation_id    VARCHAR(255) NOT NULL DEFAULT 'test-corr-id',
    quantity          NUMERIC   NOT NULL DEFAULT 0,
    created_by        INTEGER   NOT NULL,
    updated_by        INTEGER   NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
