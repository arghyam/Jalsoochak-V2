package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for reason list configuration.
 * Supports full CRUD: add, update, delete reasons.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class ReasonListConfigDTO implements ConfigValueDTO {
    @NotEmpty(message = "At least one reason must be provided")
    private List<@Valid ReasonItemDTO> reasons;
}
