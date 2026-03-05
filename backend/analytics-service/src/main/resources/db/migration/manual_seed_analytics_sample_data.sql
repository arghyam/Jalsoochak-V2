-- Manual seed script for analytics API testing.
-- Run this directly in PostgreSQL (psql, DBeaver, etc.). This file is intentionally
-- not prefixed with V* so Flyway does not auto-apply it as a migration.

BEGIN;

-- -----------------------------------------------------------------------------
-- Tenant schema dependencies for tenant_data API (tenant_id=1 -> state_code=MP)
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS tenant_mp;

CREATE TABLE IF NOT EXISTS tenant_mp.location_config_master_table (
    id INTEGER PRIMARY KEY,
    level INTEGER NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS tenant_mp.lgd_location_master_table (
    id INTEGER PRIMARY KEY,
    parent_id INTEGER NULL,
    lgd_location_config_id INTEGER NOT NULL,
    title VARCHAR(255) NULL,
    lgd_code VARCHAR(100) NULL,
    geom geometry(Geometry, 4326) NULL,
    status INTEGER DEFAULT 1,
    deleted_at TIMESTAMP NULL
);

-- -----------------------------------------------------------------------------
-- Cleanup (keeps script re-runnable)
-- -----------------------------------------------------------------------------
DELETE FROM analytics_schema.dim_operator_attendance_table
WHERE tenant_id IN (1, 33, 34);

DELETE FROM analytics_schema.fact_escalation_table
WHERE tenant_id IN (1, 33, 34);

DELETE FROM analytics_schema.fact_scheme_performance_table
WHERE tenant_id IN (1, 33, 34);

DELETE FROM analytics_schema.fact_water_quantity_table
WHERE tenant_id IN (1, 33, 34);

DELETE FROM analytics_schema.fact_meter_reading_table
WHERE tenant_id IN (1, 33, 34);

DELETE FROM analytics_schema.dim_scheme_table
WHERE scheme_id IN (1001, 1002, 2001, 10101, 10102);

DELETE FROM analytics_schema.dim_department_location_table
WHERE department_id IN (7000, 7100, 5000, 5100, 5110, 5120, 6000, 6100, 6110);

DELETE FROM analytics_schema.dim_lgd_location_table
WHERE lgd_id IN (1, 11, 12, 100, 110, 111, 112, 200, 210, 211);

DELETE FROM analytics_schema.dim_user_table
WHERE user_id IN (1101, 3301, 3401);

DELETE FROM analytics_schema.dim_date_table
WHERE date_key BETWEEN 20260201 AND 20260210;

-- -----------------------------------------------------------------------------
-- Date dimension (for attendance + reporting convenience)
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.dim_date_table
    (date_key, full_date, day, month, month_name, quarter, year, week, is_weekend, fiscal_year)
VALUES
    (20260201, DATE '2026-02-01', 1, 2, 'February', 1, 2026, 5, true, 2025),
    (20260202, DATE '2026-02-02', 2, 2, 'February', 1, 2026, 6, false, 2025),
    (20260203, DATE '2026-02-03', 3, 2, 'February', 1, 2026, 6, false, 2025),
    (20260204, DATE '2026-02-04', 4, 2, 'February', 1, 2026, 6, false, 2025),
    (20260205, DATE '2026-02-05', 5, 2, 'February', 1, 2026, 6, false, 2025)
ON CONFLICT (date_key) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Tenants
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.dim_tenant_table
    (tenant_id, state_code, title, country_code, status, created_at, updated_at)
VALUES
    (1, 'MP', 'Madhya Pradesh', 'IN', 1, NOW(), NOW()),
    (33, 'mh', 'Maharashtra Demo', 'IN', 1, NOW(), NOW()),
    (34, 'gj', 'Gujarat Demo', 'IN', 1, NOW(), NOW())
ON CONFLICT (tenant_id) DO UPDATE SET
    state_code = EXCLUDED.state_code,
    title = EXCLUDED.title,
    country_code = EXCLUDED.country_code,
    status = EXCLUDED.status,
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- Users
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.dim_user_table
    (user_id, tenant_id, email, user_type, created_at, updated_at)
VALUES
    (1101, 1, 'operator.mp@demo.org', 4, NOW(), NOW()),
    (3301, 33, 'operator.mh@demo.org', 4, NOW(), NOW()),
    (3401, 34, 'operator.gj@demo.org', 4, NOW(), NOW())
ON CONFLICT (user_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    email = EXCLUDED.email,
    user_type = EXCLUDED.user_type,
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- LGD hierarchy
-- tenant 1: level1(1) -> level2(11,12)
-- tenant 33: level1(100) -> level2(110) -> level3(111,112)
-- tenant 34: level1(200) -> level2(210) -> level3(211)
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.dim_lgd_location_table
    (lgd_id, tenant_id, lgd_code, lgd_c_name, title, lgd_level,
     level_1_lgd_id, level_2_lgd_id, level_3_lgd_id, level_4_lgd_id, level_5_lgd_id, level_6_lgd_id,
     geom, created_at, updated_at)
VALUES
    (1, 1, 'MP-STATE', 'mp_state', 'MP State', 1, 1, NULL, NULL, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((77.0 23.0,77.5 23.0,77.5 23.5,77.0 23.5,77.0 23.0))', 4326), NOW(), NOW()),
    (11, 1, 'MP-DIST-1', 'mp_dist_1', 'MP District 1', 2, 1, 11, NULL, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((77.05 23.05,77.25 23.05,77.25 23.25,77.05 23.25,77.05 23.05))', 4326), NOW(), NOW()),
    (12, 1, 'MP-DIST-2', 'mp_dist_2', 'MP District 2', 2, 1, 12, NULL, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((77.26 23.06,77.45 23.06,77.45 23.24,77.26 23.24,77.26 23.06))', 4326), NOW(), NOW()),
    (100, 33, 'MH-STATE', 'mh_state', 'MH State', 1, 100, NULL, NULL, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((72.0 18.0,72.4 18.0,72.4 18.4,72.0 18.4,72.0 18.0))', 4326), NOW(), NOW()),
    (110, 33, 'MH-DIST-1', 'mh_dist_1', 'MH District 1', 2, 100, 110, NULL, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((72.05 18.05,72.35 18.05,72.35 18.35,72.05 18.35,72.05 18.05))', 4326), NOW(), NOW()),
    (111, 33, 'MH-BLOCK-1', 'mh_block_1', 'MH Block 1', 3, 100, 110, 111, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((72.08 18.08,72.20 18.08,72.20 18.20,72.08 18.20,72.08 18.08))', 4326), NOW(), NOW()),
    (112, 33, 'MH-BLOCK-2', 'mh_block_2', 'MH Block 2', 3, 100, 110, 112, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((72.22 18.22,72.32 18.22,72.32 18.32,72.22 18.32,72.22 18.22))', 4326), NOW(), NOW()),
    (200, 34, 'GJ-STATE', 'gj_state', 'GJ State', 1, 200, NULL, NULL, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((73.0 21.0,73.4 21.0,73.4 21.4,73.0 21.4,73.0 21.0))', 4326), NOW(), NOW()),
    (210, 34, 'GJ-DIST-1', 'gj_dist_1', 'GJ District 1', 2, 200, 210, NULL, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((73.05 21.05,73.35 21.05,73.35 21.35,73.05 21.35,73.05 21.05))', 4326), NOW(), NOW()),
    (211, 34, 'GJ-BLOCK-1', 'gj_block_1', 'GJ Block 1', 3, 200, 210, 211, NULL, NULL, NULL,
     ST_GeomFromText('POLYGON((73.10 21.10,73.20 21.10,73.20 21.20,73.10 21.20,73.10 21.10))', 4326), NOW(), NOW())
ON CONFLICT (lgd_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    lgd_code = EXCLUDED.lgd_code,
    lgd_c_name = EXCLUDED.lgd_c_name,
    title = EXCLUDED.title,
    lgd_level = EXCLUDED.lgd_level,
    level_1_lgd_id = EXCLUDED.level_1_lgd_id,
    level_2_lgd_id = EXCLUDED.level_2_lgd_id,
    level_3_lgd_id = EXCLUDED.level_3_lgd_id,
    level_4_lgd_id = EXCLUDED.level_4_lgd_id,
    level_5_lgd_id = EXCLUDED.level_5_lgd_id,
    level_6_lgd_id = EXCLUDED.level_6_lgd_id,
    geom = EXCLUDED.geom,
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- Department hierarchy
-- tenant 1: level1(7000) -> level2(7100)
-- tenant 33: level1(5000) -> level2(5100) -> level3(5110,5120)
-- tenant 34: level1(6000) -> level2(6100) -> level3(6110)
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.dim_department_location_table
    (department_id, tenant_id, department_c_name, title, department_level,
     level_1_dept_id, level_2_dept_id, level_3_dept_id, level_4_dept_id, level_5_dept_id, level_6_dept_id,
     created_at, updated_at, geom)
VALUES
    (7000, 1, 'mp_department_l1', 'MP Dept L1', 1,
     7000, NULL, NULL, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((77.0 23.0,77.4 23.0,77.4 23.4,77.0 23.4,77.0 23.0))', 4326)),
    (7100, 1, 'mp_department_l2', 'MP Dept L2', 2,
     7000, 7100, NULL, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((77.08 23.08,77.32 23.08,77.32 23.32,77.08 23.32,77.08 23.08))', 4326)),
    (5000, 33, 'mh_department_l1', 'MH Dept L1', 1,
     5000, NULL, NULL, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((72.0 18.0,72.3 18.0,72.3 18.3,72.0 18.3,72.0 18.0))', 4326)),
    (5100, 33, 'mh_department_l2', 'MH Dept L2', 2,
     5000, 5100, NULL, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((72.05 18.05,72.28 18.05,72.28 18.28,72.05 18.28,72.05 18.05))', 4326)),
    (5110, 33, 'mh_department_l3_a', 'MH Dept Child A', 3,
     5000, 5100, 5110, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((72.08 18.08,72.18 18.08,72.18 18.18,72.08 18.18,72.08 18.08))', 4326)),
    (5120, 33, 'mh_department_l3_b', 'MH Dept Child B', 3,
     5000, 5100, 5120, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((72.19 18.19,72.27 18.19,72.27 18.27,72.19 18.27,72.19 18.19))', 4326)),
    (6000, 34, 'gj_department_l1', 'GJ Dept L1', 1,
     6000, NULL, NULL, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((73.0 21.0,73.3 21.0,73.3 21.3,73.0 21.3,73.0 21.0))', 4326)),
    (6100, 34, 'gj_department_l2', 'GJ Dept L2', 2,
     6000, 6100, NULL, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((73.05 21.05,73.25 21.05,73.25 21.25,73.05 21.25,73.05 21.05))', 4326)),
    (6110, 34, 'gj_department_l3', 'GJ Dept Child A', 3,
     6000, 6100, 6110, NULL, NULL, NULL, NOW(), NOW(),
     ST_GeomFromText('POLYGON((73.10 21.10,73.18 21.10,73.18 21.18,73.10 21.18,73.10 21.10))', 4326))
ON CONFLICT (department_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    department_c_name = EXCLUDED.department_c_name,
    title = EXCLUDED.title,
    department_level = EXCLUDED.department_level,
    level_1_dept_id = EXCLUDED.level_1_dept_id,
    level_2_dept_id = EXCLUDED.level_2_dept_id,
    level_3_dept_id = EXCLUDED.level_3_dept_id,
    level_4_dept_id = EXCLUDED.level_4_dept_id,
    level_5_dept_id = EXCLUDED.level_5_dept_id,
    level_6_dept_id = EXCLUDED.level_6_dept_id,
    geom = EXCLUDED.geom,
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- Schemes (with household and FHTC counts)
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.dim_scheme_table
    (scheme_id, tenant_id, scheme_name, state_scheme_id, centre_scheme_id, longitude, latitude,
     parent_lgd_location_id, level_1_lgd_id, level_2_lgd_id, level_3_lgd_id, level_4_lgd_id, level_5_lgd_id, level_6_lgd_id,
     parent_department_location_id, level_1_dept_id, level_2_dept_id, level_3_dept_id, level_4_dept_id, level_5_dept_id, level_6_dept_id,
     status, created_at, updated_at, fhtc_count, planned_fhtc, house_hold_count)
VALUES
    (10101, 1, 'Scheme MP-A', 101001, 191001, 77.14, 23.14,
     1, 1, 11, NULL, NULL, NULL, NULL,
     7100, 7000, 7100, NULL, NULL, NULL, NULL,
     1, NOW(), NOW(), 85, 100, 100),
    (10102, 1, 'Scheme MP-B', 101002, 191002, 77.33, 23.15,
     1, 1, 12, NULL, NULL, NULL, NULL,
     7100, 7000, 7100, NULL, NULL, NULL, NULL,
     1, NOW(), NOW(), 72, 90, 90),
    (1001, 33, 'Scheme MH-A', 33001, 93001, 72.12, 18.12,
     110, 100, 110, 111, NULL, NULL, NULL,
     5100, 5000, 5100, 5110, NULL, NULL, NULL,
     1, NOW(), NOW(), 95, 120, 120),
    (1002, 33, 'Scheme MH-B', 33002, 93002, 72.26, 18.26,
     110, 100, 110, 112, NULL, NULL, NULL,
     5100, 5000, 5100, 5120, NULL, NULL, NULL,
     1, NOW(), NOW(), 62, 80, 80),
    (2001, 34, 'Scheme GJ-A', 34001, 94001, 73.15, 21.15,
     210, 200, 210, 211, NULL, NULL, NULL,
     6100, 6000, 6100, 6110, NULL, NULL, NULL,
     1, NOW(), NOW(), 70, 100, 100)
ON CONFLICT (scheme_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    scheme_name = EXCLUDED.scheme_name,
    state_scheme_id = EXCLUDED.state_scheme_id,
    centre_scheme_id = EXCLUDED.centre_scheme_id,
    longitude = EXCLUDED.longitude,
    latitude = EXCLUDED.latitude,
    parent_lgd_location_id = EXCLUDED.parent_lgd_location_id,
    level_1_lgd_id = EXCLUDED.level_1_lgd_id,
    level_2_lgd_id = EXCLUDED.level_2_lgd_id,
    level_3_lgd_id = EXCLUDED.level_3_lgd_id,
    level_4_lgd_id = EXCLUDED.level_4_lgd_id,
    level_5_lgd_id = EXCLUDED.level_5_lgd_id,
    level_6_lgd_id = EXCLUDED.level_6_lgd_id,
    parent_department_location_id = EXCLUDED.parent_department_location_id,
    level_1_dept_id = EXCLUDED.level_1_dept_id,
    level_2_dept_id = EXCLUDED.level_2_dept_id,
    level_3_dept_id = EXCLUDED.level_3_dept_id,
    level_4_dept_id = EXCLUDED.level_4_dept_id,
    level_5_dept_id = EXCLUDED.level_5_dept_id,
    level_6_dept_id = EXCLUDED.level_6_dept_id,
    status = EXCLUDED.status,
    fhtc_count = EXCLUDED.fhtc_count,
    planned_fhtc = EXCLUDED.planned_fhtc,
    house_hold_count = EXCLUDED.house_hold_count,
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- Meter readings (used by regularity + submission-rate + avg water supply APIs)
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.fact_meter_reading_table
    (tenant_id, scheme_id, user_id, extracted_reading, confirmed_reading, confidence, image_url,
     reading_at, channel, reading_date, created_at, submission_status)
VALUES
    (1, 10101, 1101, 4900, 4800, 94, 'https://example.com/mp-a-20260201.jpg', TIMESTAMP '2026-02-01 07:50:00', 1, DATE '2026-02-01', NOW(), 1),
    (1, 10101, 1101, 5000, 5000, 95, 'https://example.com/mp-a-20260202.jpg', TIMESTAMP '2026-02-02 07:50:00', 1, DATE '2026-02-02', NOW(), 1),
    (1, 10102, 1101, 4200, 4100, 93, 'https://example.com/mp-b-20260201.jpg', TIMESTAMP '2026-02-01 07:55:00', 1, DATE '2026-02-01', NOW(), 1),
    (1, 10102, 1101, 0,    0,    88, 'https://example.com/mp-b-20260203.jpg', TIMESTAMP '2026-02-03 07:55:00', 1, DATE '2026-02-03', NOW(), 1),
    (33, 1001, 3301, 5100, 5000, 95, 'https://example.com/mh-a-20260201.jpg', TIMESTAMP '2026-02-01 08:00:00', 1, DATE '2026-02-01', NOW(), 1),
    (33, 1001, 3301, 5200, 5200, 96, 'https://example.com/mh-a-20260202.jpg', TIMESTAMP '2026-02-02 08:00:00', 1, DATE '2026-02-02', NOW(), 1),
    (33, 1001, 3301, 0,    0,    90, 'https://example.com/mh-a-20260203.jpg', TIMESTAMP '2026-02-03 08:00:00', 1, DATE '2026-02-03', NOW(), 1),
    (33, 1001, 3301, 5300, 5300, 95, 'https://example.com/mh-a-20260204.jpg', TIMESTAMP '2026-02-04 08:00:00', 1, DATE '2026-02-04', NOW(), 1),
    (33, 1002, 3301, 4100, 4000, 92, 'https://example.com/mh-b-20260201.jpg', TIMESTAMP '2026-02-01 09:00:00', 1, DATE '2026-02-01', NOW(), 1),
    (33, 1002, 3301, 0,    0,    88, 'https://example.com/mh-b-20260202.jpg', TIMESTAMP '2026-02-02 09:00:00', 1, DATE '2026-02-02', NOW(), 1),
    (34, 2001, 3401, 6200, 6000, 94, 'https://example.com/gj-a-20260201.jpg', TIMESTAMP '2026-02-01 10:00:00', 1, DATE '2026-02-01', NOW(), 1),
    (34, 2001, 3401, 6300, 6200, 94, 'https://example.com/gj-a-20260203.jpg', TIMESTAMP '2026-02-03 10:00:00', 1, DATE '2026-02-03', NOW(), 1);

-- -----------------------------------------------------------------------------
-- Water quantity fact
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.fact_water_quantity_table
    (tenant_id, scheme_id, user_id, water_quantity, date, created_at, updated_at)
VALUES
    (1, 10101, 1101, 4800, DATE '2026-02-01', NOW(), NOW()),
    (1, 10102, 1101, 4100, DATE '2026-02-01', NOW(), NOW()),
    (33, 1001, 3301, 5000, DATE '2026-02-01', NOW(), NOW()),
    (33, 1001, 3301, 5200, DATE '2026-02-02', NOW(), NOW()),
    (33, 1002, 3301, 4000, DATE '2026-02-01', NOW(), NOW()),
    (34, 2001, 3401, 6000, DATE '2026-02-01', NOW(), NOW());

-- -----------------------------------------------------------------------------
-- Escalations
-- resolution_status: 0=open, 1=resolved (example)
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.fact_escalation_table
    (tenant_id, scheme_id, escalation_type, message, user_id, resolution_status, remark, created_at, updated_at)
VALUES
    (1, 10101, 1, 'Supply drop MP', 1101, 0, 'Pending verification', NOW(), NOW()),
    (33, 1001, 1, 'Low pressure reported', 3301, 0, 'Pending field visit', NOW(), NOW()),
    (33, 1002, 2, 'Pump fault', 3301, 1, 'Repaired and closed', NOW(), NOW()),
    (34, 2001, 1, 'Intermittent supply', 3401, 0, 'Monitoring', NOW(), NOW());

-- -----------------------------------------------------------------------------
-- Scheme performance
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.fact_scheme_performance_table
    (scheme_id, tenant_id, performance_score, last_water_supply_date, created_at, updated_at)
VALUES
    (10101, 1, 74, DATE '2026-02-02', NOW(), NOW()),
    (10102, 1, 69, DATE '2026-02-03', NOW(), NOW()),
    (1001, 33, 82, DATE '2026-02-04', NOW(), NOW()),
    (1002, 33, 68, DATE '2026-02-01', NOW(), NOW()),
    (2001, 34, 76, DATE '2026-02-03', NOW(), NOW());

-- -----------------------------------------------------------------------------
-- Optional operator attendance
-- -----------------------------------------------------------------------------
INSERT INTO analytics_schema.dim_operator_attendance_table
    (tenant_id, date_key, user_id, scheme_id, attendance, remark, remark_by, created_at, updated_at)
VALUES
    (1, 20260201, 1101, 10101, 1, 'Present', 1101, NOW(), NOW()),
    (33, 20260201, 3301, 1001, 1, 'Present', 3301, NOW(), NOW()),
    (33, 20260202, 3301, 1002, 1, 'Present', 3301, NOW(), NOW()),
    (34, 20260201, 3401, 2001, 1, 'Present', 3401, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- tenant_mp schema rows for tenant_data API
-- -----------------------------------------------------------------------------
INSERT INTO tenant_mp.location_config_master_table (id, level, deleted_at)
VALUES
    (1, 1, NULL),
    (2, 2, NULL)
ON CONFLICT (id) DO UPDATE SET
    level = EXCLUDED.level,
    deleted_at = EXCLUDED.deleted_at;

INSERT INTO tenant_mp.lgd_location_master_table
    (id, parent_id, lgd_location_config_id, title, lgd_code, geom, status, deleted_at)
VALUES
    (1, NULL, 1, 'MP State', 'MP-STATE', ST_GeomFromText('POLYGON((77.0 23.0,77.5 23.0,77.5 23.5,77.0 23.5,77.0 23.0))', 4326), 1, NULL),
    (11, 1, 2, 'MP District 1', 'MP-DIST-1', ST_GeomFromText('POLYGON((77.05 23.05,77.25 23.05,77.25 23.25,77.05 23.25,77.05 23.05))', 4326), 1, NULL),
    (12, 1, 2, 'MP District 2', 'MP-DIST-2', ST_GeomFromText('POLYGON((77.26 23.06,77.45 23.06,77.45 23.24,77.26 23.24,77.26 23.06))', 4326), 1, NULL)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    lgd_location_config_id = EXCLUDED.lgd_location_config_id,
    title = EXCLUDED.title,
    lgd_code = EXCLUDED.lgd_code,
    geom = EXCLUDED.geom,
    status = EXCLUDED.status,
    deleted_at = EXCLUDED.deleted_at;

COMMIT;
