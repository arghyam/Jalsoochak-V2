-- ============================================================
-- Migration: V5 - Bind invite tokens to tenant
-- ============================================================

ALTER TABLE common_schema.invite_tokens
    ADD COLUMN IF NOT EXISTS tenant_id INTEGER;

DO $$
DECLARE
    s RECORD;
BEGIN
    FOR s IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant_%'
    LOOP
        EXECUTE format(
            'UPDATE common_schema.invite_tokens it
             SET tenant_id = u.tenant_id
             FROM %I.user_table u
             WHERE it.tenant_id IS NULL
               AND it.sender_id = u.id',
            s.nspname
        );
    END LOOP;
END$$;

ALTER TABLE common_schema.invite_tokens
    ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invite_tokens_tenant_id
    ON common_schema.invite_tokens (tenant_id);
