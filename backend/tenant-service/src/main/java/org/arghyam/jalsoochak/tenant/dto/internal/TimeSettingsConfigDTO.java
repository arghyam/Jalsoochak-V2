package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for time-based configuration settings.
 * Used for reminder times, escalation times, consolidation times, etc.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class TimeSettingsConfigDTO implements ConfigValueDTO {
    @NotBlank(message = "Time value is required")
    @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "Time must be in HH:mm format (e.g., 14:30)")
    private String timeValue;
    
    private String description;
}
