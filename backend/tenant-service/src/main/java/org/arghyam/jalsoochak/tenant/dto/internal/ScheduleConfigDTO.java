package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reusable DTO for time schedule configuration.
 * Used across multiple configuration types that require hour and minute specification.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleConfigDTO {
    @NotNull(message = "Hour is required")
    @Min(value = 0, message = "Hour must be between 0 and 23")
    @Max(value = 23, message = "Hour must be between 0 and 23")
    private Integer hour;
    
    @NotNull(message = "Minute is required")
    @Min(value = 0, message = "Minute must be between 0 and 59")
    @Max(value = 59, message = "Minute must be between 0 and 59")
    private Integer minute;
}
