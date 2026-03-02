-- ============================================================
-- DIM OPERATOR ATTENDANCE TABLE
-- ============================================================

CREATE TABLE analytics_schema.dim_operator_attendance_table (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    date_key    INT          NOT NULL REFERENCES analytics_schema.dim_date_table(date_key),
    user_id     INT          NOT NULL REFERENCES analytics_schema.dim_user_table(user_id),
    scheme_id   INT          NOT NULL REFERENCES analytics_schema.dim_scheme_table(scheme_id),
    attendance  INT          NOT NULL,
    remark      TEXT,
    remark_by   INT          REFERENCES analytics_schema.dim_user_table(user_id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE INDEX idx_dim_op_attendance_tenant_id ON analytics_schema.dim_operator_attendance_table(tenant_id);
CREATE INDEX idx_dim_op_attendance_date_key  ON analytics_schema.dim_operator_attendance_table(date_key);
CREATE INDEX idx_dim_op_attendance_user_id   ON analytics_schema.dim_operator_attendance_table(user_id);
CREATE INDEX idx_dim_op_attendance_scheme_id ON analytics_schema.dim_operator_attendance_table(scheme_id);
