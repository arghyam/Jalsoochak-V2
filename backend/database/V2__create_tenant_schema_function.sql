-- ============================================================
-- Migration: V2 - Tenant Schema Provisioning Function
-- Creates a reusable PL/pgSQL function that provisions an
-- identical set of tables for every new tenant.
--
-- Usage:
--   SELECT create_tenant_schema('tenant_mp');   -- Madhya Pradesh
--   SELECT create_tenant_schema('tenant_up');   -- Uttar Pradesh
--
-- Convention: schema name = tenant_<state_code_lowercase>
-- ============================================================

CREATE OR REPLACE FUNCTION create_tenant_schema(schema_name TEXT)
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

    -- Idempotency: skip if already provisioned
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = schema_name
          AND table_name   = 'scheme_master_table'
    ) THEN
        RAISE NOTICE 'Tenant schema "%" is already provisioned. Skipping.', schema_name;
        RETURN;
    END IF;

    -- ============================================================
    -- 1. Create Schema
    -- ============================================================

    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

    -- ============================================================
    -- 2. Tables
    --    (ENUM values are managed in application code, stored as VARCHAR)
    -- ============================================================

    -- --------------------------------------------------------
    -- 2.1  user_table
    --      Tenant-level users (pani samiti members, pump operators, etc.)
    --      Referenced as FK(user_table) by several tenant tables.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.user_table (
            id              SERIAL          PRIMARY KEY,
            uuid            UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            name            VARCHAR(255)    NOT NULL,
            email           VARCHAR(255),
            phone_number    TEXT            NOT NULL,   -- stored encrypted
            user_type_id    INTEGER         NOT NULL,
            password        TEXT            NOT NULL,   -- stored hashed
            status          VARCHAR(20)     NOT NULL,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by      INTEGER,
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by      INTEGER,

            CONSTRAINT fk_user_type
                FOREIGN KEY (user_type_id)
                REFERENCES common_schema.user_type_master_table(id),
            CONSTRAINT fk_user_created_by
                FOREIGN KEY (created_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),
            CONSTRAINT fk_user_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.2  lgd_location_master_table
    --      LGD (Local Government Directory) location hierarchy
    --      for this tenant (state → district → block → GP → village).
    --      Self-referencing via parent_id.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.lgd_location_master_table (
            id                          SERIAL          PRIMARY KEY,
            uuid                        UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            title                       VARCHAR(255)    NOT NULL,
            lgd_code                    VARCHAR(50)     NOT NULL,
            lgd_location_type_level     INTEGER         NOT NULL,
            lgd_location_type_name      VARCHAR(255)    NOT NULL,   -- e.g. district, block, village
            parent_id                   INTEGER,
            house_hold_count            INTEGER         NOT NULL DEFAULT 0,  -- applicable at village level
            status                      VARCHAR(20)     NOT NULL,
            created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by                  INTEGER,
            updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by                  INTEGER,

            CONSTRAINT fk_lgd_location_type_level
                FOREIGN KEY (lgd_location_type_level)
                REFERENCES common_schema.lgd_location_type_master_table(id),
            CONSTRAINT fk_lgd_location_parent
                FOREIGN KEY (parent_id)
                REFERENCES %1$I.lgd_location_master_table(id),
            CONSTRAINT fk_lgd_location_created_by
                FOREIGN KEY (created_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),
            CONSTRAINT fk_lgd_location_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.3  department_master_table
    --      Departmental hierarchy for this tenant
    --      (e.g. zone → circle → division → sub-division).
    --      Self-referencing via parent_id.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.department_master_table (
            id                              SERIAL          PRIMARY KEY,
            uuid                            UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            title                           VARCHAR(255)    NOT NULL,
            department_location_type_level   INTEGER         NOT NULL,
            department_location_type_name    VARCHAR(255)    NOT NULL,   -- e.g. circle, division
            parent_id                       INTEGER,
            status                          VARCHAR(20)     NOT NULL,
            created_at                      TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by                      INTEGER,
            updated_at                      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by                      INTEGER,

            CONSTRAINT fk_dept_location_type_level
                FOREIGN KEY (department_location_type_level)
                REFERENCES common_schema.department_location_type_master_table(id),
            CONSTRAINT fk_dept_parent
                FOREIGN KEY (parent_id)
                REFERENCES %1$I.department_master_table(id),
            CONSTRAINT fk_dept_created_by
                FOREIGN KEY (created_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),
            CONSTRAINT fk_dept_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.4  scheme_master_table
    --      Core water-supply scheme information.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.scheme_master_table (
            id                  SERIAL              PRIMARY KEY,
            uuid                UUID                NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            state_scheme_id     VARCHAR(100)        NOT NULL,
            centre_scheme_id    VARCHAR(100)        NOT NULL,
            fhtc_count          INTEGER             NOT NULL DEFAULT 0,
            planned_fhtc        INTEGER             NOT NULL DEFAULT 0,
            house_hold_count    INTEGER             NOT NULL DEFAULT 0,
            latitude            DOUBLE PRECISION,
            longitude           DOUBLE PRECISION,
            geo_location        TEXT,               -- WKT / GeoJSON; consider PostGIS GEOGRAPHY type for spatial queries
            status              VARCHAR(20)         NOT NULL,
            created_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
            created_by          INTEGER,
            updated_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
            updated_by          INTEGER,

            CONSTRAINT fk_scheme_created_by
                FOREIGN KEY (created_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),
            CONSTRAINT fk_scheme_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.5  scheme_lgd_mapping_table
    --      Maps a scheme to one or more LGD locations.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.scheme_lgd_mapping_table (
            id                  SERIAL          PRIMARY KEY,
            uuid                UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            scheme_id           INTEGER         NOT NULL,
            parent_lgd_code     INTEGER         NOT NULL,
            parent_lgd_level    VARCHAR(255)    NOT NULL,
            created_by          INTEGER         NOT NULL,
            created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by          INTEGER         NOT NULL,
            updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_scheme_lgd_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_scheme_lgd_parent
                FOREIGN KEY (parent_lgd_code)
                REFERENCES %1$I.lgd_location_master_table(id),
            CONSTRAINT fk_scheme_lgd_created_by
                FOREIGN KEY (created_by)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_scheme_lgd_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES %1$I.user_table(id),

            CONSTRAINT uq_scheme_lgd
                UNIQUE (scheme_id, parent_lgd_code)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.6  scheme_department_mapping_table
    --      Maps a scheme to one or more departmental nodes.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.scheme_department_mapping_table (
            id                          SERIAL          PRIMARY KEY,
            uuid                        UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            scheme_id                   INTEGER         NOT NULL,
            parent_department_code      INTEGER         NOT NULL,
            parent_department_level     VARCHAR(255)    NOT NULL,
            created_by                  INTEGER         NOT NULL,
            created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by                  INTEGER         NOT NULL,
            updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_scheme_dept_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_scheme_dept_parent
                FOREIGN KEY (parent_department_code)
                REFERENCES %1$I.department_master_table(id),
            CONSTRAINT fk_scheme_dept_created_by
                FOREIGN KEY (created_by)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_scheme_dept_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES %1$I.user_table(id),

            CONSTRAINT uq_scheme_dept
                UNIQUE (scheme_id, parent_department_code)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.7  flow_reading_table
    --      Stores meter / sensor readings for a scheme.
    --      image_url stores a URL/path to object storage
    --      (S3 / MinIO), NOT base64-encoded image data.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.flow_reading_table (
            id                  SERIAL          PRIMARY KEY,
            uuid                UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            scheme_id           INTEGER         NOT NULL,
            reading_at          TIMESTAMP       NOT NULL,   -- when the reading / photo was taken
            reading_date        DATE            NOT NULL,   -- the date this reading reports for
            extracted_reading   NUMERIC         NOT NULL,
            confirmed_reading   NUMERIC         NOT NULL,
            correlation_id      VARCHAR(255)    NOT NULL,
            quantity            NUMERIC         NOT NULL DEFAULT 0,
            channel             INTEGER         NOT NULL,
            image_url           TEXT            NOT NULL DEFAULT '''',  -- plain URL, not encoded
            created_by          INTEGER         NOT NULL,
            created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by          INTEGER         NOT NULL,
            updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

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

    -- --------------------------------------------------------
    -- 2.8  user_scheme_mapping_table
    --      Assigns tenant-level users to specific schemes.
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.user_scheme_mapping_table (
            id              SERIAL          PRIMARY KEY,
            uuid            UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            user_id         INTEGER         NOT NULL,
            scheme_id       INTEGER         NOT NULL,
            status          VARCHAR(20)     NOT NULL,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            created_by      INTEGER,
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by      INTEGER,

            CONSTRAINT fk_user_scheme_user
                FOREIGN KEY (user_id)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_user_scheme_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_user_scheme_created_by
                FOREIGN KEY (created_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),
            CONSTRAINT fk_user_scheme_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),

            CONSTRAINT uq_user_scheme
                UNIQUE (user_id, scheme_id)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.9  notification_table
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.notification_table (
            id              SERIAL          PRIMARY KEY,
            uuid            UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            user_id         INTEGER         NOT NULL,
            message         TEXT,
            seen_status     BOOLEAN         NOT NULL DEFAULT FALSE,
            channel         VARCHAR(30)     NOT NULL,   -- SMS, WHATSAPP, EMAIL, PUSH_NOTIFICATION
            message_blob    TEXT            NOT NULL,
            message_type    VARCHAR(20)     NOT NULL,   -- NUDGE, ESCALATION, INFO
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_notification_user
                FOREIGN KEY (user_id)
                REFERENCES %1$I.user_table(id)
        )', schema_name);

    -- --------------------------------------------------------
    -- 2.10 anomaly_table
    -- --------------------------------------------------------

    EXECUTE format('
        CREATE TABLE %1$I.anomaly_table (
            id              SERIAL          PRIMARY KEY,
            uuid            UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
            user_id         INTEGER,
            scheme_id       INTEGER,
            type            VARCHAR(30)         NOT NULL,   -- USER_ACTIVITY, IMAGE_RELATED, SCHEME_RELATED, FAKE_DATA
            detail          TEXT                NOT NULL,
            status          VARCHAR(20)         NOT NULL,
            created_at      TIMESTAMP           NOT NULL DEFAULT NOW(),

            CONSTRAINT fk_anomaly_user
                FOREIGN KEY (user_id)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_anomaly_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id)
        )', schema_name);

    -- ============================================================
    -- 3. Indexes
    -- ============================================================

    -- user_table
    EXECUTE format('CREATE INDEX idx_%1$s_user_status       ON %1$I.user_table(status)',       schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_user_type         ON %1$I.user_table(user_type_id)', schema_name);

    -- lgd_location_master_table
    EXECUTE format('CREATE INDEX idx_%1$s_lgd_code          ON %1$I.lgd_location_master_table(lgd_code)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_lgd_parent        ON %1$I.lgd_location_master_table(parent_id)',  schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_lgd_status        ON %1$I.lgd_location_master_table(status)',     schema_name);

    -- department_master_table
    EXECUTE format('CREATE INDEX idx_%1$s_dept_parent       ON %1$I.department_master_table(parent_id)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_dept_status       ON %1$I.department_master_table(status)',    schema_name);

    -- scheme_master_table
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_status     ON %1$I.scheme_master_table(status)',           schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_state_id   ON %1$I.scheme_master_table(state_scheme_id)',  schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_scheme_centre_id  ON %1$I.scheme_master_table(centre_scheme_id)', schema_name);

    -- scheme_lgd_mapping_table
    EXECUTE format('CREATE INDEX idx_%1$s_slm_scheme        ON %1$I.scheme_lgd_mapping_table(scheme_id)',      schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_slm_lgd           ON %1$I.scheme_lgd_mapping_table(parent_lgd_code)',schema_name);

    -- scheme_department_mapping_table
    EXECUTE format('CREATE INDEX idx_%1$s_sdm_scheme        ON %1$I.scheme_department_mapping_table(scheme_id)',             schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_sdm_dept          ON %1$I.scheme_department_mapping_table(parent_department_code)',schema_name);

    -- flow_reading_table
    EXECUTE format('CREATE INDEX idx_%1$s_flow_scheme       ON %1$I.flow_reading_table(scheme_id)',      schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_flow_date         ON %1$I.flow_reading_table(reading_date)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_flow_channel      ON %1$I.flow_reading_table(channel)',        schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_flow_corr         ON %1$I.flow_reading_table(correlation_id)', schema_name);

    -- user_scheme_mapping_table
    EXECUTE format('CREATE INDEX idx_%1$s_usm_user          ON %1$I.user_scheme_mapping_table(user_id)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_usm_scheme        ON %1$I.user_scheme_mapping_table(scheme_id)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_usm_status        ON %1$I.user_scheme_mapping_table(status)',    schema_name);

    -- notification_table
    EXECUTE format('CREATE INDEX idx_%1$s_notif_user        ON %1$I.notification_table(user_id)',     schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_notif_seen        ON %1$I.notification_table(seen_status)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_notif_channel     ON %1$I.notification_table(channel)',     schema_name);

    -- anomaly_table
    EXECUTE format('CREATE INDEX idx_%1$s_anom_user         ON %1$I.anomaly_table(user_id)',   schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_anom_scheme       ON %1$I.anomaly_table(scheme_id)', schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_anom_type         ON %1$I.anomaly_table(type)',      schema_name);
    EXECUTE format('CREATE INDEX idx_%1$s_anom_status       ON %1$I.anomaly_table(status)',    schema_name);

    -- ============================================================
    -- Done
    -- ============================================================

    RAISE NOTICE 'Tenant schema "%" provisioned successfully with all tables and indexes.', schema_name;

END;
$func$;


-- ============================================================
-- Example: provision a tenant for Madhya Pradesh
-- ============================================================
-- SELECT create_tenant_schema('tenant_mp');
