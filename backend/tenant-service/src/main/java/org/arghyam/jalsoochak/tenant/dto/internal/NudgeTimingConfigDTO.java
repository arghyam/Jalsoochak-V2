package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for pump operator reminder nudge configuration.
 * Defines the schedule time at which reminder notifications are sent
 * if meter reading has not been submitted.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class NudgeTimingConfigDTO implements ConfigValueDTO {
    
    @NotNull(message = "Nudge configuration is required")
    @Valid
    private Nudge nudge;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Nudge {
        @NotNull(message = "Schedule is required")
        @Valid
        private ScheduleConfigDTO schedule;
    }
}
