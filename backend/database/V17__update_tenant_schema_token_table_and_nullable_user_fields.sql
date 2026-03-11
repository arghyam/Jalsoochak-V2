-- V17: Replace user_invite_table with user_token_table in tenant schemas,
--      and make user_table.email and user_table.password nullable to support
--      field staff (operators, inspectors) who have no email or password.
--
-- Part A: Rename user_invite_table → user_token_table in all existing tenant schemas,
--         and drop NOT NULL from user_table.email and user_table.password.
-- Part B: Patch create_tenant_schema() so new tenants get user_token_table
--         and nullable email/password on user_table.

-- ── Part A: Rename in existing tenant schemas ──────────────────────────────

DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    FOR tenant_schema IN
        SELECT nspname FROM pg_namespace WHERE nspname LIKE 'tenant\_%' ESCAPE '\'
    LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = tenant_schema AND table_name = 'user_invite_table') THEN
            EXECUTE format('ALTER TABLE %I.user_invite_table RENAME TO user_token_table', tenant_schema);
            -- Add new token columns
            EXECUTE format('ALTER TABLE %I.user_token_table
                ADD COLUMN IF NOT EXISTS email       VARCHAR(255),
                ADD COLUMN IF NOT EXISTS token_hash  VARCHAR(64),
                ADD COLUMN IF NOT EXISTS token_type  VARCHAR(20),
                ADD COLUMN IF NOT EXISTS used_at     TIMESTAMPTZ,
                ADD COLUMN IF NOT EXISTS expires_at  TIMESTAMPTZ', tenant_schema);
            -- Backfill email from user_table (user_id was NOT NULL in old schema)
            EXECUTE format('UPDATE %1$I.user_token_table utt
                SET email = u.email
                FROM %1$I.user_table u
                WHERE utt.user_id = u.id AND utt.email IS NULL', tenant_schema);
            -- Backfill token_type and expires_at for legacy invite rows; mark expired so they cannot be consumed
            EXECUTE format('UPDATE %1$I.user_token_table
                SET token_type = ''INVITE'',
                    token_hash = LPAD(TO_HEX(id), 64, ''0''),
                    expires_at = NOW() - INTERVAL ''1 second'',
                    used_at    = COALESCE(used_at, NOW())
                WHERE token_type IS NULL', tenant_schema);
            -- Enforce NOT NULL now that all rows are backfilled
            EXECUTE format('ALTER TABLE %1$I.user_token_table
                ALTER COLUMN token_hash SET NOT NULL,
                ALTER COLUMN token_type SET NOT NULL,
                ALTER COLUMN expires_at SET NOT NULL', tenant_schema);
            -- Create the same indexes as Part B so existing schemas match new tenants
            EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS idx_%1$s_ut_hash  ON %1$I.user_token_table(token_hash)', tenant_schema);
            EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS uq_%1$s_ut_active ON %1$I.user_token_table(email, token_type) WHERE used_at IS NULL AND deleted_at IS NULL', tenant_schema);
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%1$s_ut_email ON %1$I.user_token_table(email)', tenant_schema);
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%1$s_ut_type  ON %1$I.user_token_table(token_type)', tenant_schema);
        END IF;

        -- Always apply regardless of user_invite_table presence:
        -- make email and password nullable to support field staff without credentials.
        EXECUTE format('ALTER TABLE %I.user_table ALTER COLUMN email    DROP NOT NULL', tenant_schema);
        EXECUTE format('ALTER TABLE %I.user_table ALTER COLUMN password DROP NOT NULL', tenant_schema);
    END LOOP;
END $$;

-- ── Part B: Replace create_tenant_schema() with definitive version ────────
-- This is a complete, self-contained rewrite that consolidates all changes
-- from V2 through V17, eliminating the brittle wrapper chain from V7/V10.

-- Clean up the broken public-schema wrapper left by V7.
DROP FUNCTION IF EXISTS public.create_tenant_schema(TEXT);
DROP FUNCTION IF EXISTS public.create_tenant_schema_v2_base(TEXT);

CREATE OR REPLACE FUNCTION common_schema.create_tenant_schema(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $func$
BEGIN

    -- ============================================================
    -- Guard clauses
    -- ============================================================
    IF schema_name IS NULL OR schema_name = '' THEN
        RAISE EXCEPTION 'schema_name must not be null or empty';
    END IF;

    IF schema_name !~ '^[a-z_][a-z0-9_]*$' THEN
        RAISE EXCEPTION 'schema_name must be a valid lowercase identifier (got: %)', schema_name;
    END IF;

    -- Idempotency: skip if schema already exists (guards against partial re-runs too,
    -- since all CREATE TABLE/INDEX statements below use IF NOT EXISTS).
    IF EXISTS (
        SELECT 1 FROM pg_namespace WHERE nspname = schema_name
    ) THEN
        RAISE NOTICE 'Tenant schema "%" already exists. Skipping.', schema_name;
        RETURN;
    END IF;

    -- ============================================================
    -- 1. Create Schema
    -- ============================================================

    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

    -- ============================================================
    -- 2. Tables
    -- ============================================================

    EXECUTE format('
        CREATE TABLE %1$I.location_config_master_table (
            id              SERIAL          PRIMARY KEY,
            uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            region_type     INTEGER         NOT NULL,
            level           INTEGER         NOT NULL,
            level_name      JSONB           NOT NULL,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by      INTEGER,
            updated_by      INTEGER,
            deleted_at      TIMESTAMP,
            deleted_by      INTEGER
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %1$I.user_table (
            id                          SERIAL          PRIMARY KEY,
            uuid                        VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            tenant_id                   INTEGER         NOT NULL,
            title                       TEXT            NOT NULL,
            email                       VARCHAR(255)    UNIQUE,
            user_type                   INTEGER         NOT NULL,
            phone_number                TEXT            NOT NULL,
            password                    TEXT,
            status                      INTEGER         NOT NULL,
            email_verification_status   BOOLEAN         DEFAULT FALSE,
            phone_verification_status   BOOLEAN         DEFAULT FALSE,
            language_id                 INTEGER,
            created_by                  INTEGER,
            created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by                  INTEGER,
            updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            deleted_at                  TIMESTAMP,
            deleted_by                  INTEGER
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.lgd_location_master_table (
            id                      SERIAL          PRIMARY KEY,
            uuid                    VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            title                   VARCHAR(255)    NOT NULL,
            lgd_code                VARCHAR(50)     NOT NULL,
            lgd_location_config_id  INTEGER,
            parent_id               INTEGER,
            house_hold_count        NUMERIC         NOT NULL DEFAULT 0,
            created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by              INTEGER,
            updated_by              INTEGER,
            status                  INTEGER         NOT NULL,
            deleted_at              TIMESTAMP,
            deleted_by              INTEGER,

            CONSTRAINT fk_lgd_location_config
                FOREIGN KEY (lgd_location_config_id)
                REFERENCES %1$I.location_config_master_table(id),
            CONSTRAINT fk_lgd_location_parent
                FOREIGN KEY (parent_id)
                REFERENCES %1$I.lgd_location_master_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.department_location_master_table (
            id                              SERIAL          PRIMARY KEY,
            uuid                            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            title                           VARCHAR(255)    NOT NULL,
            department_location_config_id   INTEGER,
            parent_id                       INTEGER,
            created_at                      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at                      TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by                      INTEGER,
            updated_by                      INTEGER,
            status                          INTEGER         NOT NULL,
            deleted_at                      TIMESTAMP,
            deleted_by                      INTEGER,

            CONSTRAINT fk_dept_location_config
                FOREIGN KEY (department_location_config_id)
                REFERENCES %1$I.location_config_master_table(id),
            CONSTRAINT fk_dept_parent
                FOREIGN KEY (parent_id)
                REFERENCES %1$I.department_location_master_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %1$I.scheme_master_table (
            id                  SERIAL              PRIMARY KEY,
            uuid                VARCHAR(36)         NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            state_scheme_id     VARCHAR(255)        NOT NULL,
            centre_scheme_id    VARCHAR(255)        NOT NULL,
            scheme_name         VARCHAR(255)        NOT NULL,
            fhtc_count          INTEGER             NOT NULL DEFAULT 0,
            planned_fhtc        INTEGER             NOT NULL DEFAULT 0,
            house_hold_count    INTEGER             NOT NULL DEFAULT 0,
            latitude            DOUBLE PRECISION,
            longitude           DOUBLE PRECISION,
            channel             INTEGER,
            work_status         INTEGER             NOT NULL,
            operating_status    INTEGER             NOT NULL,
            created_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
            created_by          INTEGER,
            updated_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
            updated_by          INTEGER,
            deleted_at          TIMESTAMP,
            deleted_by          INTEGER
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.scheme_lgd_mapping_table (
            id                  SERIAL          PRIMARY KEY,
            scheme_id           INTEGER         NOT NULL,
            parent_lgd_id       INTEGER         NOT NULL,
            parent_lgd_level    VARCHAR(255)    NOT NULL,
            created_by          INTEGER         NOT NULL,
            created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by          INTEGER         NOT NULL,
            updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
            deleted_at          TIMESTAMP,
            deleted_by          INTEGER,

            CONSTRAINT fk_scheme_lgd_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_scheme_lgd_parent
                FOREIGN KEY (parent_lgd_id)
                REFERENCES %1$I.lgd_location_master_table(id),
            CONSTRAINT fk_scheme_lgd_created_by
                FOREIGN KEY (created_by)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_scheme_lgd_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES %1$I.user_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.scheme_department_mapping_table (
            id                          SERIAL          PRIMARY KEY,
            scheme_id                   INTEGER         NOT NULL,
            parent_department_id        INTEGER         NOT NULL,
            parent_department_level     VARCHAR(255)    NOT NULL,
            created_by                  INTEGER         NOT NULL,
            created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by                  INTEGER         NOT NULL,
            updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            deleted_at                  TIMESTAMP,
            deleted_by                  INTEGER,

            CONSTRAINT fk_scheme_dept_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_scheme_dept_parent
                FOREIGN KEY (parent_department_id)
                REFERENCES %1$I.department_location_master_table(id),
            CONSTRAINT fk_scheme_dept_created_by
                FOREIGN KEY (created_by)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_scheme_dept_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES %1$I.user_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.flow_reading_table (
            id                          SERIAL          PRIMARY KEY,
            uuid                        VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            scheme_id                   INTEGER         NOT NULL,
            reading_at                  TIMESTAMP       NOT NULL,
            reading_date                DATE            NOT NULL,
            extracted_reading           NUMERIC         NOT NULL,
            confirmed_reading           NUMERIC         NOT NULL,
            correlation_id              VARCHAR(255)    NOT NULL,
            quantity                    NUMERIC         NOT NULL DEFAULT 0,
            channel                     INTEGER,
            ai_confidence_percentage    NUMERIC,
            latitude                    DOUBLE PRECISION,
            longitude                   DOUBLE PRECISION,
            meter_change_reason         TEXT,
            issue_report_reason         TEXT,
            image_url                   TEXT            DEFAULT '''',
            created_by                  INTEGER         NOT NULL,
            created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by                  INTEGER         NOT NULL,
            updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            deleted_at                  TIMESTAMP,
            deleted_by                  INTEGER,

            CONSTRAINT fk_flow_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_flow_created_by
                FOREIGN KEY (created_by)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_flow_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES %1$I.user_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.user_scheme_mapping_table (
            id              SERIAL          PRIMARY KEY,
            uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            user_id         INTEGER         NOT NULL,
            scheme_id       INTEGER         NOT NULL,
            status          INTEGER         NOT NULL,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by      INTEGER,
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by      INTEGER,
            deleted_at      TIMESTAMP,
            deleted_by      INTEGER,

            CONSTRAINT fk_user_scheme_user
                FOREIGN KEY (user_id)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_user_scheme_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.notification_table (
            id              SERIAL          PRIMARY KEY,
            uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            user_id         INTEGER         NOT NULL,
            message         TEXT,
            seen_status     BOOLEAN         NOT NULL DEFAULT FALSE,
            channel         INTEGER         NOT NULL,
            message_blob    TEXT            NOT NULL,
            message_type    VARCHAR(50)     NOT NULL,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            deleted_at      TIMESTAMP,
            deleted_by      INTEGER,

            CONSTRAINT fk_notification_user
                FOREIGN KEY (user_id)
                REFERENCES %1$I.user_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.anomaly_table (
            id              SERIAL          PRIMARY KEY,
            uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            user_id         INTEGER,
            scheme_id       INTEGER,
            type            INTEGER         NOT NULL,
            detail          TEXT            NOT NULL,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            status          INTEGER         NOT NULL,
            remark          TEXT,
            resolved_by     INTEGER,
            resolved_at     TIMESTAMP,
            deleted_at      TIMESTAMP,
            deleted_by      INTEGER,

            CONSTRAINT fk_anomaly_user
                FOREIGN KEY (user_id)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_anomaly_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id)
        )', schema_name);

    EXECUTE format('
        CREATE TABLE %1$I.language_master_table (
            id              SERIAL          PRIMARY KEY,
            language_name   VARCHAR(255)    NOT NULL,
            preference      INTEGER,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            status          INTEGER         NOT NULL,
            created_by      INTEGER,
            updated_by      INTEGER,
            deleted_at      TIMESTAMP,
            deleted_by      INTEGER,
            CONSTRAINT uq_language_name UNIQUE (language_name)
        )', schema_name);

    -- --------------------------------------------------------
    -- user_token_table
    --   Stores opaque invite and password-reset tokens for
    --   tenant-level users. User record is created at invite
    --   time (status = PENDING); user_id is always set.
    -- --------------------------------------------------------
    EXECUTE format('
        CREATE TABLE %1$I.user_token_table (
            id          BIGSERIAL    PRIMARY KEY,
            uuid        VARCHAR(36)  NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            email       VARCHAR(255) NOT NULL,
            user_id     INTEGER      NOT NULL,
            token_hash  VARCHAR(64)  NOT NULL,
            token_type  VARCHAR(20)  NOT NULL,
            expires_at  TIMESTAMP    NOT NULL,
            used_at     TIMESTAMP,
            deleted_at  TIMESTAMP,
            deleted_by  INTEGER,
            created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
            created_by  INTEGER,

            CONSTRAINT fk_ut_user
                FOREIGN KEY (user_id) REFERENCES %1$I.user_table(id)
        )', schema_name);

    -- ============================================================
    -- 3. Indexes
    -- ============================================================

    -- user_table
    EXECUTE format('CREATE INDEX idx_%1$s_user_status        ON %1$I.user_table(status)',       schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_user_type          ON %1$I.user_table(user_type)',     schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_user_tenant        ON %1$I.user_table(tenant_id)',     schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_user_language_id   ON %1$I.user_table(language_id)',   schema_name);

    -- lgd_location_master_table
    EXECUTE format('CREATE INDEX idx_%1$s_lgd_code           ON %1$I.lgd_location_master_table(lgd_code)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_lgd_parent         ON %1$I.lgd_location_master_table(parent_id)',  schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_lgd_status         ON %1$I.lgd_location_master_table(status)',     schema_name);

    -- department_location_master_table
    EXECUTE format('CREATE INDEX idx_%1$s_dept_parent        ON %1$I.department_location_master_table(parent_id)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_dept_status        ON %1$I.department_location_master_table(status)',    schema_name);

    -- scheme_master_table
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_work_st     ON %1$I.scheme_master_table(work_status)',       schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_op_st       ON %1$I.scheme_master_table(operating_status)',  schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_state_id    ON %1$I.scheme_master_table(state_scheme_id)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_centre_id   ON %1$I.scheme_master_table(centre_scheme_id)',  schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_channel     ON %1$I.scheme_master_table(channel)',           schema_name);

    -- scheme_lgd_mapping_table
    EXECUTE format('CREATE INDEX idx_%1$s_slm_scheme         ON %1$I.scheme_lgd_mapping_table(scheme_id)',     schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_slm_lgd            ON %1$I.scheme_lgd_mapping_table(parent_lgd_id)', schema_name);

    -- scheme_department_mapping_table
    EXECUTE format('CREATE INDEX idx_%1$s_sdm_scheme         ON %1$I.scheme_department_mapping_table(scheme_id)',            schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_sdm_dept           ON %1$I.scheme_department_mapping_table(parent_department_id)', schema_name);

    -- flow_reading_table
    EXECUTE format('CREATE INDEX idx_%1$s_flow_scheme        ON %1$I.flow_reading_table(scheme_id)',      schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_flow_date          ON %1$I.flow_reading_table(reading_date)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_flow_channel       ON %1$I.flow_reading_table(channel)',        schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_flow_corr          ON %1$I.flow_reading_table(correlation_id)', schema_name);

    -- user_scheme_mapping_table
    EXECUTE format('CREATE INDEX idx_%1$s_usm_user           ON %1$I.user_scheme_mapping_table(user_id)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_usm_scheme         ON %1$I.user_scheme_mapping_table(scheme_id)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_usm_status         ON %1$I.user_scheme_mapping_table(status)',    schema_name);

    -- notification_table
    EXECUTE format('CREATE INDEX idx_%1$s_notif_user         ON %1$I.notification_table(user_id)',     schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_notif_seen         ON %1$I.notification_table(seen_status)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_notif_channel      ON %1$I.notification_table(channel)',     schema_name);

    -- anomaly_table
    EXECUTE format('CREATE INDEX idx_%1$s_anom_user          ON %1$I.anomaly_table(user_id)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_anom_scheme        ON %1$I.anomaly_table(scheme_id)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_anom_type          ON %1$I.anomaly_table(type)',      schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_anom_status        ON %1$I.anomaly_table(status)',    schema_name);

    -- user_token_table
    EXECUTE format('CREATE UNIQUE INDEX idx_%1$s_ut_hash     ON %1$I.user_token_table(token_hash)',  schema_name);
    EXECUTE format('CREATE UNIQUE INDEX uq_%1$s_ut_active    ON %1$I.user_token_table(email, token_type) WHERE used_at IS NULL AND deleted_at IS NULL', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_ut_email           ON %1$I.user_token_table(email)',       schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_ut_type            ON %1$I.user_token_table(token_type)',  schema_name);

    -- ============================================================
    -- Done
    -- ============================================================

    RAISE NOTICE 'Tenant schema "%" provisioned successfully with all tables and indexes.', schema_name;

END;
$func$;
