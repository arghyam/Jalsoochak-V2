package org.arghyam.jalsoochak.tenant.enums;

import org.arghyam.jalsoochak.tenant.dto.internal.ChannelListConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.EmailProviderConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.EmailSenderConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.SimpleConfigValueDTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Global configuration keys for system-level platform settings.
 * Each key declares the expected DTO class for that key.
 * These are managed by Super Admins and serve as defaults.
 * Implements ConfigKey sealed interface to restrict config keys to known types.
 */
@Getter
@RequiredArgsConstructor
public enum SystemConfigKeyEnum implements ConfigKey {

    /**
     * JalSoochak Server Mode (Default: Single Tenant Mode).
     * Supported values: STM (Single Tenant Mode), MTM (Multi Tenant Mode).
     * Managed by Super Admin.
     */
    JALSOOCHAK_SERVER_MODE(SimpleConfigValueDTO.class),

    /**
     * Email Provider Connection Settings.
     * Contains SMTP configuration (host, port, username, password, TLS, timeouts).
     * Managed by Super Admin.
     */
    EMAIL_PROVIDER_CONNECTION_SETTING(EmailProviderConfigDTO.class),

    /**
     * Email Sender Identity Settings.
     * Contains from address/name and reply-to address/name.
     * Managed by Super Admin.
     */
    EMAIL_SENDER_IDENTITY_SETTING(EmailSenderConfigDTO.class),

    /**
     * System Supported Communication Channels.
     * Defines all channels supported at platform level (BFM, ELM, PDU, IOT, MAN).
     * Super User can add/remove supported channels at system level.
     */
    SYSTEM_SUPPORTED_CHANNELS(ChannelListConfigDTO.class),

    /**
     * Water Quantity Supply Threshold.
     * Percentage deviation from Water Norm above which supply is marked as inadequate.
     * Managed by Super User.
     */
    WATER_QUANTITY_SUPPLY_THRESHOLD(SimpleConfigValueDTO.class),

    /**
     * Grading Classes Settings.
     * Configurable labels, criteria, and colors for grading classes (e.g., High, Medium, Low).
     * This setting is system-wide and cannot be overridden at tenant level.
     * Managed by Super User.
     */
    GRADING_CLASSES_SETTINGS(SimpleConfigValueDTO.class),

    /**
     * BFM Image Reading Confidence Level Threshold.
     * Minimum confidence level for AI Reader (FlowVision) meter readings.
     * Readings below this threshold are marked with low confidence flag.
     * Managed by Super User.
     */
    BFM_IMAGE_READING_CONFIDENCE_LEVEL_THRESHOLD(SimpleConfigValueDTO.class),

    /**
     * Location Affinity Threshold.
     * Configurable parameter for validating whether submitted meter reading
     * is in the vicinity of the Scheme.
     * Managed by Super User.
     */
    LOCATION_AFFINITY_THRESHOLD(SimpleConfigValueDTO.class),

    /**
     * Default LGD (Local Government Directory) location hierarchy configuration.
     * Stored in common_schema.tenant_config_master_table with tenant_id = 0.
     * Default: State -> District -> Block -> Panchayat -> Village.
     * Tenants can override this at tenant level.
     */
    DEFAULT_LGD_LOCATION_HIERARCHY(LocationConfigDTO.class),

    /**
     * Default department location hierarchy configuration.
     * Stored in common_schema.tenant_config_master_table with tenant_id = 0.
     * Default: State -> Zone -> Circle -> Division -> Sub-division.
     * Tenants can override this at tenant level.
     */
    DEFAULT_DEPT_LOCATION_HIERARCHY(LocationConfigDTO.class);

    private final Class<? extends ConfigValueDTO> dtoClass;

}
