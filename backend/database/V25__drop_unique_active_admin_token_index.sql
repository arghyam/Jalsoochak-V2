-- V25: Support multiple active invite/reset tokens per user (invalidate-on-use, not on-issue).
--
-- Previously, unique partial indexes enforced at most one active (not used, not deleted)
-- token per (email, token_type), which meant reinviting or re-requesting a password reset
-- always immediately invalidated the previous link — even if the user had not received the
-- first email yet.
--
-- New behaviour:
--   - On reinvite / forgot-password: a new token is inserted without touching existing ones.
--     Both the old and new email links remain valid until TTL.
--   - On token consumption (account activation / password reset): the consumed token is
--     marked used_at = NOW(), and all other pending tokens for the same (email, token_type)
--     are marked deleted_at = NOW() atomically in the same transaction.
--
-- The deleted_at column already semantically means "this token is no longer valid"; reusing
-- it for supersession on consumption is consistent — no new column is required.
--
-- The unique constraints are replaced with regular composite indexes to preserve fast lookups
-- on active tokens without the one-per-(email, token_type) restriction.

-- ── 1. common_schema.admin_user_token_table ──────────────────────────────────

DROP INDEX common_schema.uq_active_admin_token;

CREATE INDEX idx_admin_token_active
    ON common_schema.admin_user_token_table (email, token_type)
    WHERE used_at IS NULL AND deleted_at IS NULL;

-- ── 2. Existing tenant schemas: user_token_table ─────────────────────────────

DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    FOR tenant_schema IN
        SELECT nspname FROM pg_namespace WHERE nspname LIKE 'tenant\_%' ESCAPE '\'
    LOOP
        EXECUTE format(
            'DROP INDEX IF EXISTS %I.%I',
            tenant_schema,
            'uq_' || tenant_schema || '_ut_active'
        );
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS %I ON %I.user_token_table (email, token_type) WHERE used_at IS NULL AND deleted_at IS NULL',
            'idx_' || tenant_schema || '_ut_active',
            tenant_schema
        );
    END LOOP;
END $$;

-- ── 3. Patch create_tenant_schema() ──────────────────────────────────────────
-- Replace only the unique index line for user_token_table so new tenants also
-- get a non-unique index. The rest of the function is left unchanged.

DO $$
DECLARE
    func_src TEXT;
BEGIN
    SELECT prosrc INTO func_src
    FROM pg_proc
    WHERE proname = 'create_tenant_schema'
      AND pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'common_schema');

    func_src := replace(
        func_src,
        'CREATE UNIQUE INDEX uq_%1$s_ut_active    ON %1$I.user_token_table(email, token_type) WHERE used_at IS NULL AND deleted_at IS NULL',
        'CREATE INDEX idx_%1$s_ut_active          ON %1$I.user_token_table(email, token_type) WHERE used_at IS NULL AND deleted_at IS NULL'
    );

    EXECUTE format(
        $q$CREATE OR REPLACE FUNCTION common_schema.create_tenant_schema(schema_name TEXT)
           RETURNS VOID LANGUAGE plpgsql AS $body$%s$body$$q$,
        func_src
    );
END $$;
