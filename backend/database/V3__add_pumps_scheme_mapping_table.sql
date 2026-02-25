-- ============================================================
-- Migration: V3 - Add pumps_scheme_mapping_table to all tenants
-- ============================================================

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
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %1$I.pumps_scheme_mapping_table (
                id              SERIAL          PRIMARY KEY,
                uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
                pump_model      VARCHAR(255),
                capacity        INTEGER         NOT NULL,
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
            )', tenant_schema);

        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%1$s_psm_scheme ON %1$I.pumps_scheme_mapping_table(scheme_id)',
            tenant_schema
        );
    END LOOP;
END $$;

-- 2) Ensure all future tenant schemas also get this table.
--    We preserve the existing function by renaming it and wrapping it.
DO $$
BEGIN
    IF to_regprocedure('create_tenant_schema_v2_backup(text)') IS NULL THEN
        IF to_regprocedure('create_tenant_schema(text)') IS NULL THEN
            RAISE EXCEPTION 'create_tenant_schema(text) not found';
        END IF;

        EXECUTE 'ALTER FUNCTION create_tenant_schema(text) RENAME TO create_tenant_schema_v2_backup';
    END IF;
END $$;

CREATE OR REPLACE FUNCTION create_tenant_schema(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $func$
BEGIN
    -- Call original tenant provisioning logic first.
    PERFORM create_tenant_schema_v2_backup(schema_name);

    -- Ensure pumps mapping table exists for this tenant schema.
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %1$I.pumps_scheme_mapping_table (
            id              SERIAL          PRIMARY KEY,
            uuid            VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
            pump_model      VARCHAR(255),
            capacity        INTEGER         NOT NULL,
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
        'CREATE INDEX IF NOT EXISTS idx_%1$s_psm_scheme ON %1$I.pumps_scheme_mapping_table(scheme_id)',
        schema_name
    );
END;
$func$;
