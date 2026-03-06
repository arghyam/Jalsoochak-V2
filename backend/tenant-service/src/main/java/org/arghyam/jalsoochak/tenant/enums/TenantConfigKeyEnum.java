package org.arghyam.jalsoochak.tenant.enums;

import org.arghyam.jalsoochak.tenant.dto.internal.ChannelListConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.DateFormatConfigDTO;

import org.arghyam.jalsoochak.tenant.dto.internal.MessageBrokerConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LanguageListConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ReasonListConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.SimpleConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.StateITSystemConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.TimeSettingsConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.NudgeTimingConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.EscalationRulesConfigDTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Exhaustive list of allowed configuration keys for tenants.
 * Each key declares its storage type and the expected DTO class for that key.
 * Implements ConfigKey sealed interface to restrict config keys to known types.
 */
@Getter
@RequiredArgsConstructor
public enum TenantConfigKeyEnum implements ConfigKey {

    /**
     * Date format for screen display.
     * Format: DD-MON-YYYY with 24H time format and timezone (e.g., Asia/Kolkata).
     */
    DATE_FORMAT_SCREEN(ConfigType.GENERIC, DateFormatConfigDTO.class),

    /**
     * Date format for table display.
     * Format: DD/MM/YYYY with 24H time format and timezone (e.g., Asia/Kolkata).
     */
    DATE_FORMAT_TABLE(ConfigType.GENERIC, DateFormatConfigDTO.class),

    /**
     * Supported communication channels for this tenant.
     * Subset of channels defined at system level (SYSTEM_SUPPORTED_CHANNELS).
     * State Admin can enable/disable channels: BFM, ELM, PDU, IOT, MAN.
     */
    TENANT_SUPPORTED_CHANNELS(ConfigType.GENERIC, ChannelListConfigDTO.class),

    /**
     * Tenant logo file.
     */
    TENANT_LOGO(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * Meter change reasons list with CRUD capability.
     * Default reasons: Meter Replaced, Meter Not Working, Meter Damaged.
     * State Admin can add, update, and delete reasons.
     */
    METER_CHANGE_REASONS(ConfigType.GENERIC, ReasonListConfigDTO.class),

    /**
     * Supported languages for the tenant (up to 4 languages with preference order).
     * Stored in tenant-specific language_master_table.
     */
    SUPPORTED_LANGUAGES(ConfigType.SPECIALIZED, LanguageListConfigDTO.class),

    /**
     * Location verification requirement flag.
     * Yes or No. Default: No.
     * Determines if GPS location check is required for meter readings.
     */
    LOCATION_CHECK_REQUIRED(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * Glific WhatsApp messages and prompts configuration.
     * Customizable messages for specific cases (e.g., data upload/modification).
     */
    GLIFIC_WHATSAPP_MESSAGES_AND_PROMPTS(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * Glific Connection Settings.
     * Contains API credentials and endpoints for WhatsApp integration.
     */
    MESSAGE_BROKER_CONNECTION_SETTINGS(ConfigType.GENERIC, MessageBrokerConfigDTO.class),

    /**
     * State IT Systems API Endpoint and Credentials.
     * For integration with state-level systems and data exchange.
     */
    STATE_IT_SYSTEM_CONNECTION(ConfigType.GENERIC, StateITSystemConfigDTO.class),

    /**
     * Water Norm for this tenant.
     * Example: 55 LPCD (Liters Per Capita Per Day).
     */
    WATER_NORM(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * Minimum Water Quantity Supply threshold.
     * Quantity (in litres or kilo-liters) below which water supply for a day
     * is considered as No Supply.
     */
    TENANT_WATER_QUANTITY_SUPPLY_THRESHOLD(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * Pump Operator Reminder Nudge Configuration.
     * Defines the absolute time by which reminder message is sent if meter reading not submitted.
     * Format: { nudge: { schedule: { hour: 8, minute: 0 } } }
     */
    PUMP_OPERATOR_REMINDER_NUDGE_TIME(ConfigType.GENERIC, NudgeTimingConfigDTO.class),

    /**
     * Field Staff Escalation Rules.
     * Defines escalation schedule and multi-level escalation rules based on thresholds and officer types.
     * Escalation messages are sent to field staff (Section Officer, District Officer,ExecutiveEngineer, etc.).
     */
    FIELD_STAFF_ESCALATION_RULES(ConfigType.GENERIC, EscalationRulesConfigDTO.class),

    /**
     * Data Consolidation Time.
     * Absolute time on which the data consolidation cron job runs.
     */
    DATA_CONSOLIDATION_TIME(ConfigType.GENERIC, TimeSettingsConfigDTO.class),

    /**
     * State Data Reconciliation Time.
     * Absolute time for state-level data reconciliation process.
     */
    STATE_DATA_RECONCILIATION_TIME(ConfigType.GENERIC, TimeSettingsConfigDTO.class),

    /**
     * JSON definition for email templates.
     */
    EMAIL_TEMPLATE_JSON(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * Display Department Maps flag.
     * YES or NO. Controls whether department dashboards show maps.
     * Used for states where map boundaries data is unavailable.
     */
    DISPLAY_DEPARTMENT_MAPS(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * Average Members Per Household.
     * Positive numeric value (decimals allowed) used as multiplier for
     * calculating family members count for FHTC/Household.
     */
    AVERAGE_MEMBERS_PER_HOUSEHOLD(ConfigType.GENERIC, SimpleConfigValueDTO.class),

    /**
     * LGD (Local Government Directory) location hierarchy configuration.
     * Stored in tenant-specific location_config_master_table with region_type LGD.
     * Default: System level LGD Hierarchy, can be overridden at tenant level.
     */
    LGD_LOCATION_HIERARCHY(ConfigType.SPECIALIZED, LocationConfigDTO.class),

    /**
     * Department location hierarchy configuration.
     * Stored in tenant-specific location_config_master_table with region_type DEPARTMENT.
     * Default: System level DEPT Hierarchy, can be overridden at tenant level.
     */
    DEPT_LOCATION_HIERARCHY(ConfigType.SPECIALIZED, LocationConfigDTO.class);

    private final ConfigType type;
    private final Class<? extends ConfigValueDTO> dtoClass;

    public enum ConfigType {
        GENERIC, // Stored as KV in common_schema.tenant_config_master_table
        SPECIALIZED // Stored in specific tables in tenant schema
    }
}
