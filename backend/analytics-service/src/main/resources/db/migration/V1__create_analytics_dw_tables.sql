-- ============================================================
-- ANALYTICS DATA WAREHOUSE (SIMPLIFIED TYPE 1 MODEL)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS analytics_schema;

CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================================
-- DIM DATE TABLE
-- ============================================================

CREATE TABLE analytics_schema.dim_date_table (
    date_key    INT         PRIMARY KEY,
    full_date   DATE        NOT NULL,
    day         INT,
    month       INT,
    month_name  VARCHAR(20),
    quarter     INT,
    year        INT,
    week        INT,
    is_weekend  BOOLEAN,
    fiscal_year INT
);

-- ============================================================
-- DIM TENANT TABLE
-- ============================================================

CREATE TABLE analytics_schema.dim_tenant_table (
    tenant_id    INT          PRIMARY KEY,
    state_code   VARCHAR(10)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    country_code VARCHAR(5)   DEFAULT 'IN',
    status       INT          NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP
);

-- ============================================================
-- DIM USER TABLE
-- ============================================================

CREATE TABLE analytics_schema.dim_user_table (
    user_id    INT          PRIMARY KEY,
    tenant_id  INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    email      VARCHAR(255),
    user_type  INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============================================================
-- DIM LGD LOCATION TABLE
-- ============================================================

CREATE TABLE analytics_schema.dim_lgd_location_table (
    lgd_id         INT          PRIMARY KEY,
    tenant_id      INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    lgd_code       VARCHAR(50),
    lgd_c_name     VARCHAR(255),
    title          VARCHAR(255),
    lgd_level      INT          NOT NULL,
    level_1_lgd_id INT          NOT NULL,
    level_2_lgd_id INT          NOT NULL,
    level_3_lgd_id INT          NOT NULL,
    level_4_lgd_id INT          NOT NULL,
    level_5_lgd_id INT          NOT NULL,
    level_6_lgd_id INT          NOT NULL,
    geom           GEOMETRY,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

-- ============================================================
-- DIM DEPARTMENT LOCATION TABLE
-- ============================================================

CREATE TABLE analytics_schema.dim_department_location_table (
    department_id      INT          PRIMARY KEY,
    tenant_id          INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    department_c_name  VARCHAR(255),
    title              VARCHAR(255),
    department_level   INT          NOT NULL,
    level_1_dept_id    INT          NOT NULL,
    level_2_dept_id    INT          NOT NULL,
    level_3_dept_id    INT          NOT NULL,
    level_4_dept_id    INT          NOT NULL,
    level_5_dept_id    INT          NOT NULL,
    level_6_dept_id    INT          NOT NULL,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
);

-- ============================================================
-- DIM SCHEME TABLE
-- ============================================================

CREATE TABLE analytics_schema.dim_scheme_table (
    scheme_id                    INT          PRIMARY KEY,
    tenant_id                    INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    scheme_name                  VARCHAR(255),
    state_scheme_id              INT          NOT NULL,
    centre_scheme_id             INT          NOT NULL,
    longitude                    DOUBLE PRECISION,
    latitude                     DOUBLE PRECISION,

    parent_lgd_location_id       INT          NOT NULL,
    level_1_lgd_id               INT          NOT NULL,
    level_2_lgd_id               INT          NOT NULL,
    level_3_lgd_id               INT          NOT NULL,
    level_4_lgd_id               INT          NOT NULL,
    level_5_lgd_id               INT          NOT NULL,
    level_6_lgd_id               INT          NOT NULL,

    parent_department_location_id INT         NOT NULL,
    level_1_dept_id              INT          NOT NULL,
    level_2_dept_id              INT          NOT NULL,
    level_3_dept_id              INT          NOT NULL,
    level_4_dept_id              INT          NOT NULL,
    level_5_dept_id              INT          NOT NULL,
    level_6_dept_id              INT          NOT NULL,

    status                       INT, -- 1: ACTIVE, 0: INACTIVE
    created_at                   TIMESTAMP,
    updated_at                   TIMESTAMP
);

-- ============================================================
-- FACT METER READING TABLE
-- ============================================================

CREATE TABLE analytics_schema.fact_meter_reading_table (
    id                 BIGSERIAL    PRIMARY KEY,
    tenant_id          INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    scheme_id          INT          NOT NULL REFERENCES analytics_schema.dim_scheme_table(scheme_id),
    user_id            INT          NOT NULL,
    extracted_reading  INT          NOT NULL,
    confirmed_reading  INT          NOT NULL,
    confidence         INT,
    image_url          TEXT,
    reading_at         TIMESTAMP    NOT NULL,
    channel            INT,
    reading_date       DATE         NOT NULL,
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- FACT WATER QUANTITY TABLE
-- ============================================================

CREATE TABLE analytics_schema.fact_water_quantity_table (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    scheme_id       INT          NOT NULL REFERENCES analytics_schema.dim_scheme_table(scheme_id),
    user_id         INT          NOT NULL,
    water_quantity  INT          NOT NULL,
    date            DATE         NOT NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

-- ============================================================
-- FACT ESCALATION TABLE
-- ============================================================

CREATE TABLE analytics_schema.fact_escalation_table (
    id                 BIGSERIAL    PRIMARY KEY,
    tenant_id          INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    scheme_id          INT          NOT NULL REFERENCES analytics_schema.dim_scheme_table(scheme_id),
    escalation_type    INT,
    message            TEXT,
    user_id            INT          NOT NULL,
    resolution_status  INT,
    remark             TEXT,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
);

-- ============================================================
-- FACT SCHEME PERFORMANCE TABLE
-- ============================================================

CREATE TABLE analytics_schema.fact_scheme_performance_table (
    id                      BIGSERIAL    PRIMARY KEY,
    scheme_id               INT          NOT NULL REFERENCES analytics_schema.dim_scheme_table(scheme_id),
    tenant_id               INT          NOT NULL REFERENCES analytics_schema.dim_tenant_table(tenant_id),
    performance_score       INT,
    last_water_supply_date  DATE,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Fact: meter reading
CREATE INDEX idx_fact_meter_tenant   ON analytics_schema.fact_meter_reading_table(tenant_id);
CREATE INDEX idx_fact_meter_scheme   ON analytics_schema.fact_meter_reading_table(scheme_id);
CREATE INDEX idx_fact_meter_date     ON analytics_schema.fact_meter_reading_table(reading_date);

-- Fact: water quantity
CREATE INDEX idx_fact_water_tenant   ON analytics_schema.fact_water_quantity_table(tenant_id);
CREATE INDEX idx_fact_water_scheme   ON analytics_schema.fact_water_quantity_table(scheme_id);
CREATE INDEX idx_fact_water_date     ON analytics_schema.fact_water_quantity_table(date);

-- Fact: escalation
CREATE INDEX idx_fact_esc_tenant     ON analytics_schema.fact_escalation_table(tenant_id);
CREATE INDEX idx_fact_esc_scheme     ON analytics_schema.fact_escalation_table(scheme_id);

-- Fact: scheme performance
CREATE INDEX idx_fact_perf_tenant    ON analytics_schema.fact_scheme_performance_table(tenant_id);
CREATE INDEX idx_fact_perf_scheme    ON analytics_schema.fact_scheme_performance_table(scheme_id);

-- Dimensions
CREATE INDEX idx_dim_user_tenant     ON analytics_schema.dim_user_table(tenant_id);
CREATE INDEX idx_dim_lgd_tenant      ON analytics_schema.dim_lgd_location_table(tenant_id);
CREATE INDEX idx_dim_dept_tenant     ON analytics_schema.dim_department_location_table(tenant_id);
CREATE INDEX idx_dim_scheme_tenant   ON analytics_schema.dim_scheme_table(tenant_id);

-- Spatial index on LGD geometry
CREATE INDEX idx_dim_lgd_geom        ON analytics_schema.dim_lgd_location_table USING GIST(geom);
