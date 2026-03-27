-- ============================================================
-- Migration: V17 - Add language_master_table to common_schema
-- ============================================================

CREATE TABLE IF NOT EXISTS common_schema.language_master_table (
    id           SERIAL          PRIMARY KEY,
    uuid         VARCHAR(36)     NOT NULL UNIQUE DEFAULT gen_random_uuid()::TEXT,
    label        VARCHAR(255)    NOT NULL UNIQUE,  -- e.g. Hindi, English, Marathi
    label_locale VARCHAR(255)    NOT NULL,         -- e.g. हिन्दी, தமிழ்
    locale       VARCHAR(20)     NOT NULL UNIQUE,  -- BCP-47 code e.g. en, hi, ta
    localized    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by   INTEGER,
    updated_by   INTEGER,
    deleted_at   TIMESTAMP,
    deleted_by   INTEGER,

    CONSTRAINT fk_language_created_by
        FOREIGN KEY (created_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_language_updated_by
        FOREIGN KEY (updated_by) REFERENCES common_schema.tenant_admin_user_master_table(id),
    CONSTRAINT fk_language_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES common_schema.tenant_admin_user_master_table(id)
);

CREATE INDEX IF NOT EXISTS idx_language_master_locale    ON common_schema.language_master_table(locale);
CREATE INDEX IF NOT EXISTS idx_language_master_is_active ON common_schema.language_master_table(is_active);

-- Seed languages (explicit IDs preserved to match upstream reference data)
INSERT INTO common_schema.language_master_table (id, label, label_locale, locale, localized, is_active)
OVERRIDING SYSTEM VALUE
VALUES
    (1,  'English',   'English',    'en',  true,  true),
    (2,  'Hindi',     'हिंदी',        'hi',  true,  true),
    (3,  'Tamil',     'தமிழ்',        'ta',  false, true),
    (4,  'Kannada',   'ಕನ್ನಡ',        'kn',  false, true),
    (5,  'Malayalam', 'മലയാളം',      'ml',  false, true),
    (6,  'Telugu',    'తెలుగు',       'te',  false, true),
    (7,  'Odia',      'ଓଡ଼ିଆ',        'or',  false, true),
    (8,  'Assamese',  'অসমীয়া',      'as',  false, true),
    (9,  'Gujarati',  'ગુજરાતી',      'gu',  false, true),
    (10, 'Bengali',   'বাংলা',        'bn',  false, true),
    (11, 'Punjabi',   'ਪੰਜਾਬੀ',       'pa',  false, true),
    (12, 'Marathi',   'मराठी',        'mr',  false, true),
    (13, 'Urdu',      'اردو',         'ur',  false, true),
    (21, 'Gondi',     'Koitur',       'gon', false, true)
ON CONFLICT DO NOTHING;

-- Advance sequence past the highest explicit ID
SELECT setval(
    pg_get_serial_sequence('common_schema.language_master_table', 'id'),
    (SELECT MAX(id) FROM common_schema.language_master_table)
);
