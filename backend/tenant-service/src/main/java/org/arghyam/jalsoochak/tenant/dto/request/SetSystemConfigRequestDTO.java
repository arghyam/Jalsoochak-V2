package org.arghyam.jalsoochak.tenant.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;

import java.util.Map;

/**
 * Request DTO for setting or updating platform-wide platform configurations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetSystemConfigRequestDTO {

    /**
     * Map of configuration keys and their corresponding values.
     */
    @NotEmpty(message = "Configurations map cannot be empty")
    private Map<SystemConfigKeyEnum, String> configs;
}
