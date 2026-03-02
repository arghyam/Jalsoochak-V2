-- ============================================================
-- Migration: V8 - Update anomaly_table for existing and new tenants
-- ============================================================

DO $$
DECLARE
    tenant_schema TEXT;
    has_required_nulls BOOLEAN;
BEGIN
    FOR tenant_schema IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant\_%' ESCAPE '\'
    LOOP
        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS ai_reading NUMERIC',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS ai_confidence_percentage NUMERIC',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS overridden_reading NUMERIC',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS retries INTEGER',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS previous_reading NUMERIC',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS previous_reading_date TIMESTAMP',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS consecutive_days_overridden INTEGER',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS reason TEXT',
            tenant_schema
        );

        EXECUTE format(
            'ALTER TABLE IF EXISTS %1$I.anomaly_table
             ADD COLUMN IF NOT EXISTS remarks TEXT',
            tenant_schema
        );

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = tenant_schema
              AND table_name = 'anomaly_table'
              AND column_name = 'detail'
        ) THEN
            EXECUTE format(
                'UPDATE %1$I.anomaly_table
                 SET reason = COALESCE(reason, detail)',
                tenant_schema
            );
            EXECUTE format(
                'ALTER TABLE %1$I.anomaly_table
                 DROP COLUMN detail',
                tenant_schema
            );
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = tenant_schema
              AND table_name = 'anomaly_table'
              AND column_name = 'remark'
        ) THEN
            EXECUTE format(
                'UPDATE %1$I.anomaly_table
                 SET remarks = COALESCE(remarks, remark)',
                tenant_schema
            );
            EXECUTE format(
                'ALTER TABLE %1$I.anomaly_table
                 DROP COLUMN remark',
                tenant_schema
            );
        END IF;

        EXECUTE format(
            'ALTER TABLE %1$I.anomaly_table
             ALTER COLUMN retries SET DEFAULT 0,
             ALTER COLUMN consecutive_days_overridden SET DEFAULT 0',
            tenant_schema
        );

        EXECUTE format(
            'UPDATE %1$I.anomaly_table
             SET retries = COALESCE(retries, 0),
                 consecutive_days_overridden = COALESCE(consecutive_days_overridden, 0)',
            tenant_schema
        );

        EXECUTE format(
            'SELECT EXISTS (
                 SELECT 1
                 FROM %1$I.anomaly_table
                 WHERE user_id IS NULL
                    OR scheme_id IS NULL
                    OR type IS NULL
                    OR created_at IS NULL
                    OR status IS NULL
             )',
            tenant_schema
        ) INTO has_required_nulls;

        IF NOT has_required_nulls THEN
            EXECUTE format(
                'ALTER TABLE %1$I.anomaly_table
                 ALTER COLUMN user_id SET NOT NULL,
                 ALTER COLUMN scheme_id SET NOT NULL,
                 ALTER COLUMN type SET NOT NULL,
                 ALTER COLUMN created_at SET NOT NULL,
                 ALTER COLUMN status SET NOT NULL',
                tenant_schema
            );
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE n.nspname = tenant_schema
              AND t.relname = 'anomaly_table'
              AND c.conname = 'fk_anomaly_resolved_by'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %1$I.anomaly_table
                 ADD CONSTRAINT fk_anomaly_resolved_by
                 FOREIGN KEY (resolved_by)
                 REFERENCES %1$I.user_table(id)',
                tenant_schema
            );
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE n.nspname = tenant_schema
              AND t.relname = 'anomaly_table'
              AND c.conname = 'fk_anomaly_deleted_by'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %1$I.anomaly_table
                 ADD CONSTRAINT fk_anomaly_deleted_by
                 FOREIGN KEY (deleted_by)
                 REFERENCES common_schema.tenant_admin_user_master_table(id)',
                tenant_schema
            );
        END IF;
    END LOOP;
END $$;

CREATE OR REPLACE FUNCTION public.create_tenant_schema(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $func$
BEGIN
    -- Execute the original provisioning logic first.
    PERFORM public.create_tenant_schema_v2_base(schema_name);

    -- Enforce flow_reading_table columns.
    EXECUTE format(
        'ALTER TABLE IF EXISTS %1$I.flow_reading_table
         ADD COLUMN IF NOT EXISTS ai_confidence_percentage NUMERIC',
        schema_name
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %1$I.flow_reading_table
         ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION',
        schema_name
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %1$I.flow_reading_table
         ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION',
        schema_name
    );

    -- Enforce anomaly_table structure for new tenants.
    EXECUTE format(
        'ALTER TABLE IF EXISTS %1$I.anomaly_table
         ADD COLUMN IF NOT EXISTS ai_reading NUMERIC,
         ADD COLUMN IF NOT EXISTS ai_confidence_percentage NUMERIC,
         ADD COLUMN IF NOT EXISTS overridden_reading NUMERIC,
         ADD COLUMN IF NOT EXISTS retries INTEGER,
         ADD COLUMN IF NOT EXISTS previous_reading NUMERIC,
         ADD COLUMN IF NOT EXISTS previous_reading_date TIMESTAMP,
         ADD COLUMN IF NOT EXISTS consecutive_days_overridden INTEGER,
         ADD COLUMN IF NOT EXISTS reason TEXT,
         ADD COLUMN IF NOT EXISTS remarks TEXT',
        schema_name
    );

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'anomaly_table'
          AND column_name = 'detail'
    ) THEN
        EXECUTE format(
            'UPDATE %1$I.anomaly_table
             SET reason = COALESCE(reason, detail)',
            schema_name
        );
        EXECUTE format(
            'ALTER TABLE %1$I.anomaly_table
             DROP COLUMN detail',
            schema_name
        );
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = schema_name
          AND table_name = 'anomaly_table'
          AND column_name = 'remark'
    ) THEN
        EXECUTE format(
            'UPDATE %1$I.anomaly_table
             SET remarks = COALESCE(remarks, remark)',
            schema_name
        );
        EXECUTE format(
            'ALTER TABLE %1$I.anomaly_table
             DROP COLUMN remark',
            schema_name
        );
    END IF;

    EXECUTE format(
        'ALTER TABLE %1$I.anomaly_table
         ALTER COLUMN retries SET DEFAULT 0,
         ALTER COLUMN consecutive_days_overridden SET DEFAULT 0',
        schema_name
    );

    EXECUTE format(
        'UPDATE %1$I.anomaly_table
         SET retries = COALESCE(retries, 0),
             consecutive_days_overridden = COALESCE(consecutive_days_overridden, 0)',
        schema_name
    );

    EXECUTE format(
        'ALTER TABLE %1$I.anomaly_table
         ALTER COLUMN user_id SET NOT NULL,
         ALTER COLUMN scheme_id SET NOT NULL,
         ALTER COLUMN type SET NOT NULL,
         ALTER COLUMN created_at SET NOT NULL,
         ALTER COLUMN status SET NOT NULL',
        schema_name
    );

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = schema_name
          AND t.relname = 'anomaly_table'
          AND c.conname = 'fk_anomaly_resolved_by'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %1$I.anomaly_table
             ADD CONSTRAINT fk_anomaly_resolved_by
             FOREIGN KEY (resolved_by)
             REFERENCES %1$I.user_table(id)',
            schema_name
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = schema_name
          AND t.relname = 'anomaly_table'
          AND c.conname = 'fk_anomaly_deleted_by'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %1$I.anomaly_table
             ADD CONSTRAINT fk_anomaly_deleted_by
             FOREIGN KEY (deleted_by)
             REFERENCES common_schema.tenant_admin_user_master_table(id)',
            schema_name
        );
    END IF;
END;
$func$;
