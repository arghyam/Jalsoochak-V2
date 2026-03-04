-- ============================================================
-- Migration: V5 - Add channel_type to channel_master_table
-- ============================================================

ALTER TABLE common_schema.channel_master_table
    ADD COLUMN IF NOT EXISTS channel_type INTEGER; -- 1- communication(e.g. BFM, ELCTRICT_METER etc) , 2: data_collection(e.g. WHATSAPP, API, SMS etc.)
