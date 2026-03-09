package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO wrapper for location hierarchy that implements ConfigValueDTO.
 * Wraps the existing LocationLevelConfigDTO list.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class LocationConfigDTO implements ConfigValueDTO {
    @NotEmpty(message = "At least one location level is required")
    private List<@Valid LocationLevelConfigDTO> locationHierarchy;
}
