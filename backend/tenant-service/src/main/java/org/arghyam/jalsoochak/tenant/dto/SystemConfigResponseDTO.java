package org.arghyam.jalsoochak.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;

import java.util.Map;

/**
 * Response DTO containing platform-wide platform configurations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigResponseDTO {

    /**
     * Map of configuration keys and their current values.
     */
    private Map<SystemConfigKeyEnum, String> configs;
}
