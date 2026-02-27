-- ============================================================
-- Migration: V4 - Update pumps_scheme_mapping_table structure
-- ============================================================

-- Centralized helper to keep table shape consistent for both
-- existing and future tenant schemas.
CREATE OR REPLACE FUNCTION ensure_pumps_scheme_mapping_table(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $func$
BEGIN
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %1$I.pumps_scheme_mapping_table (
            id              SERIAL          PRIMARY KEY,
            uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            pump_model      VARCHAR(255),
            efficiency      FLOAT,
            head            FLOAT,
            discharge_rate  FLOAT,
            scheme_id       INTEGER         NOT NULL,
            status          INTEGER         NOT NULL,
            created_at      TIMESTAMP       DEFAULT NOW(),
            updated_at      TIMESTAMP       DEFAULT NOW(),
            created_by      INTEGER,
            updated_by      INTEGER,
            deleted_at      TIMESTAMP,
            deleted_by      INTEGER,

            CONSTRAINT fk_pumps_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_pumps_created_by
                FOREIGN KEY (created_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),
            CONSTRAINT fk_pumps_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id),
            CONSTRAINT fk_pumps_deleted_by
                FOREIGN KEY (deleted_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id)
        )', schema_name);

    EXECUTE format(
        'ALTER TABLE %1$I.pumps_scheme_mapping_table
             ADD COLUMN IF NOT EXISTS efficiency FLOAT,
             ADD COLUMN IF NOT EXISTS head FLOAT,
             ADD COLUMN IF NOT EXISTS discharge_rate FLOAT,
             ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
             ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
             ADD COLUMN IF NOT EXISTS created_by INTEGER,
             ADD COLUMN IF NOT EXISTS updated_by INTEGER,
             ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
             ADD COLUMN IF NOT EXISTS deleted_by INTEGER',
        schema_name
    );

    -- Backfill from old V3 column when present.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'pumps_scheme_mapping_table'
          AND column_name = 'capacity'
    ) THEN
        EXECUTE format(
            'UPDATE %1$I.pumps_scheme_mapping_table
             SET discharge_rate = capacity::FLOAT
             WHERE discharge_rate IS NULL',
            schema_name
        );
    END IF;

    EXECUTE format(
        'ALTER TABLE %1$I.pumps_scheme_mapping_table
             DROP COLUMN IF EXISTS capacity',
        schema_name
    );

    -- Keep required nullability contract.
    EXECUTE format(
        'ALTER TABLE %1$I.pumps_scheme_mapping_table
             ALTER COLUMN scheme_id SET NOT NULL,
             ALTER COLUMN status SET NOT NULL',
        schema_name
    );

    -- Ensure FK constraints exist even if table pre-existed.
    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.pumps_scheme_mapping_table
               ADD CONSTRAINT fk_pumps_scheme
               FOREIGN KEY (scheme_id)
               REFERENCES %1$I.scheme_master_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.pumps_scheme_mapping_table
               ADD CONSTRAINT fk_pumps_created_by
               FOREIGN KEY (created_by)
               REFERENCES common_schema.tenant_admin_user_master_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.pumps_scheme_mapping_table
               ADD CONSTRAINT fk_pumps_updated_by
               FOREIGN KEY (updated_by)
               REFERENCES common_schema.tenant_admin_user_master_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.pumps_scheme_mapping_table
               ADD CONSTRAINT fk_pumps_deleted_by
               FOREIGN KEY (deleted_by)
               REFERENCES common_schema.tenant_admin_user_master_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%1$s_psm_scheme ON %1$I.pumps_scheme_mapping_table(scheme_id)',
        schema_name
    );
END;
$func$;

-- Centralized helper to enforce latest flow_reading_table shape.
CREATE OR REPLACE FUNCTION ensure_flow_reading_table(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $func$
BEGIN
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %1$I.flow_reading_table (
            id                  SERIAL          PRIMARY KEY,
            uuid                VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            scheme_id           INTEGER         NOT NULL,
            observation_time    TIMESTAMP       NOT NULL,
            reading_date        DATE            NOT NULL,
            correlation_id      VARCHAR(255),
            quantity            NUMERIC         NOT NULL DEFAULT 0,
            quality_flag        VARCHAR(20)     NOT NULL DEFAULT ''provisional'',
            status              VARCHAR(20)     NOT NULL DEFAULT ''active'',
            payload_json        JSONB,
            channel             VARCHAR(50),
            reported_via        VARCHAR(50),
            duration            INTEGER,
            created_by          INTEGER         NOT NULL,
            created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_by          INTEGER,
            updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
            deleted_at          TIMESTAMP,
            deleted_by          INTEGER,

            CONSTRAINT fk_flow_scheme
                FOREIGN KEY (scheme_id)
                REFERENCES %1$I.scheme_master_table(id),
            CONSTRAINT fk_flow_created_by
                FOREIGN KEY (created_by)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_flow_updated_by
                FOREIGN KEY (updated_by)
                REFERENCES %1$I.user_table(id),
            CONSTRAINT fk_flow_deleted_by
                FOREIGN KEY (deleted_by)
                REFERENCES common_schema.tenant_admin_user_master_table(id)
        )', schema_name);

    -- Rename old V2 column to match new contract.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'flow_reading_table'
          AND column_name = 'reading_at'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'flow_reading_table'
          AND column_name = 'observation_time'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %1$I.flow_reading_table RENAME COLUMN reading_at TO observation_time',
            schema_name
        );
    END IF;

    EXECUTE format(
        'ALTER TABLE %1$I.flow_reading_table
             ADD COLUMN IF NOT EXISTS observation_time TIMESTAMP,
             ADD COLUMN IF NOT EXISTS reading_date DATE,
             ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255),
             ADD COLUMN IF NOT EXISTS quantity NUMERIC DEFAULT 0,
             ADD COLUMN IF NOT EXISTS quality_flag VARCHAR(20) DEFAULT ''provisional'',
             ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT ''active'',
             ADD COLUMN IF NOT EXISTS payload_json JSONB,
             ADD COLUMN IF NOT EXISTS channel VARCHAR(50),
             ADD COLUMN IF NOT EXISTS reported_via VARCHAR(50),
             ADD COLUMN IF NOT EXISTS duration INTEGER,
             ADD COLUMN IF NOT EXISTS created_by INTEGER,
             ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
             ADD COLUMN IF NOT EXISTS updated_by INTEGER,
             ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
             ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
             ADD COLUMN IF NOT EXISTS deleted_by INTEGER',
        schema_name
    );

    -- Backfill payload JSON from legacy scalar columns.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'flow_reading_table'
          AND column_name = 'extracted_reading'
    )
    AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'flow_reading_table'
          AND column_name = 'confirmed_reading'
    ) THEN
        EXECUTE format(
            'UPDATE %1$I.flow_reading_table
             SET payload_json = COALESCE(payload_json, ''{}''::JSONB)
                                || jsonb_build_object(
                                    ''extracted_reading'', extracted_reading,
                                    ''confirmed_reading'', confirmed_reading
                                   )',
            schema_name
        );
    END IF;

    -- Ensure mandatory timestamps/dates are populated.
    EXECUTE format(
        'UPDATE %1$I.flow_reading_table
         SET observation_time = COALESCE(observation_time, created_at, NOW()),
             reading_date = COALESCE(reading_date, (COALESCE(observation_time, created_at, NOW()))::DATE),
             quantity = COALESCE(quantity, 0),
             quality_flag = COALESCE(NULLIF(quality_flag, ''''), ''provisional''),
             status = COALESCE(NULLIF(status, ''''), ''active'')',
        schema_name
    );

    -- Drop legacy scalar reading columns after payload backfill.
    EXECUTE format(
        'ALTER TABLE %1$I.flow_reading_table
             DROP COLUMN IF EXISTS extracted_reading,
             DROP COLUMN IF EXISTS confirmed_reading',
        schema_name
    );

    -- Align channel type from legacy integer to textual value when needed.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'flow_reading_table'
          AND column_name = 'channel'
          AND data_type IN ('smallint', 'integer', 'bigint', 'numeric')
    ) THEN
        EXECUTE format(
            'ALTER TABLE %1$I.flow_reading_table
             ALTER COLUMN channel TYPE VARCHAR(50) USING channel::TEXT',
            schema_name
        );
    END IF;

    -- Keep required nullability contract.
    EXECUTE format(
        'ALTER TABLE %1$I.flow_reading_table
             ALTER COLUMN uuid SET NOT NULL,
             ALTER COLUMN scheme_id SET NOT NULL,
             ALTER COLUMN observation_time SET NOT NULL,
             ALTER COLUMN reading_date SET NOT NULL,
             ALTER COLUMN quantity SET NOT NULL,
             ALTER COLUMN quality_flag SET NOT NULL,
             ALTER COLUMN status SET NOT NULL,
             ALTER COLUMN created_by SET NOT NULL,
             ALTER COLUMN created_at SET NOT NULL,
             ALTER COLUMN updated_by SET NOT NULL,
             ALTER COLUMN updated_at SET NOT NULL',
        schema_name
    );

    -- Ensure FK constraints exist even if table pre-existed.
    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.flow_reading_table
               ADD CONSTRAINT fk_flow_scheme
               FOREIGN KEY (scheme_id)
               REFERENCES %1$I.scheme_master_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.flow_reading_table
               ADD CONSTRAINT fk_flow_created_by
               FOREIGN KEY (created_by)
               REFERENCES %1$I.user_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.flow_reading_table
               ADD CONSTRAINT fk_flow_updated_by
               FOREIGN KEY (updated_by)
               REFERENCES %1$I.user_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    BEGIN
        EXECUTE format(
            'ALTER TABLE %1$I.flow_reading_table
               ADD CONSTRAINT fk_flow_deleted_by
               FOREIGN KEY (deleted_by)
               REFERENCES common_schema.tenant_admin_user_master_table(id)',
            schema_name
        );
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;

    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%1$s_flow_scheme ON %1$I.flow_reading_table(scheme_id)',
        schema_name
    );
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%1$s_flow_date ON %1$I.flow_reading_table(reading_date)',
        schema_name
    );
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%1$s_flow_channel ON %1$I.flow_reading_table(channel)',
        schema_name
    );
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%1$s_flow_corr ON %1$I.flow_reading_table(correlation_id)',
        schema_name
    );
END;
$func$;

-- 1) Backfill for already existing tenant schemas
DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    FOR tenant_schema IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant\_%' ESCAPE '\'
    LOOP
        PERFORM ensure_pumps_scheme_mapping_table(tenant_schema);
        PERFORM ensure_flow_reading_table(tenant_schema);
    END LOOP;
END $$;

-- 2) Ensure all future tenant schemas also get this updated table.
DO $$
BEGIN
    IF to_regprocedure('create_tenant_schema_v4_backup(text)') IS NULL THEN
        IF to_regprocedure('create_tenant_schema(text)') IS NULL THEN
            RAISE EXCEPTION 'create_tenant_schema(text) not found';
        END IF;

        EXECUTE 'ALTER FUNCTION create_tenant_schema(text) RENAME TO create_tenant_schema_v4_backup';
    END IF;
END $$;

CREATE OR REPLACE FUNCTION create_tenant_schema(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $func$
BEGIN
    -- Call original tenant provisioning logic first.
    PERFORM create_tenant_schema_v4_backup(schema_name);

    -- Ensure pumps mapping table exists in updated format.
    PERFORM ensure_pumps_scheme_mapping_table(schema_name);
    -- Ensure flow readings table exists in updated format.
    PERFORM ensure_flow_reading_table(schema_name);
END;
$func$;
