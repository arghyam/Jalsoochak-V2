-- V24: OTP table for phone-number + OTP staff login.
-- Lives in common_schema because login is pre-auth: tenant schema is unknown until the user is resolved.

CREATE TABLE common_schema.otp_table (
    id            SERIAL        PRIMARY KEY,
    otp           TEXT          NOT NULL,           -- AES-256-GCM encrypted via PiiEncryptionService
    tenant_id     INTEGER       NOT NULL,           -- logical FK → common_schema.tenant_master_table
    user_id       INTEGER       NOT NULL,           -- logical FK → tenant_<code>.user_table (cross-schema; no DB constraint)
    otp_type      VARCHAR(30)   NOT NULL,
    attempt_count INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ   NOT NULL,
    used_at       TIMESTAMPTZ,                      -- NULL = active; non-NULL = consumed or revoked

    CONSTRAINT chk_otp_type CHECK (otp_type IN ('LOGIN', 'PASSWORD_CHANGE'))
);

-- Enforces at most one active OTP per user+tenant+type.
-- used_at = NOW() (via markUsed/revokeActiveOtp) removes the row from the index,
-- allowing a new OTP to be inserted immediately.
CREATE UNIQUE INDEX uq_active_otp
    ON common_schema.otp_table(user_id, tenant_id, otp_type)
    WHERE used_at IS NULL;

-- Supporting indexes
CREATE INDEX idx_otp_expires ON common_schema.otp_table(expires_at);
CREATE INDEX idx_otp_tenant  ON common_schema.otp_table(tenant_id);

-- Composite partial index to speed up findActiveOtp queries (user_id + tenant_id + otp_type + expires_at, active rows only)
CREATE INDEX idx_otp_user_tenant_type_active
    ON common_schema.otp_table(user_id, tenant_id, otp_type, expires_at)
    WHERE used_at IS NULL;
