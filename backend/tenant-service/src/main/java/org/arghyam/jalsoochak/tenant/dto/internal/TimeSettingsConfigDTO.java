package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for time-based cron job configuration settings.
 * Used for data consolidation, state reconciliation, and similar scheduled tasks.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class TimeSettingsConfigDTO implements ConfigValueDTO {
    @NotNull(message = "Schedule is required")
    @Valid
    private ScheduleConfigDTO schedule;

    private String description;
}
