package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO wrapper for language configuration that implements ConfigValueDTO.
 * Wraps the list of LanguageConfigDTO (max 4 languages).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class LanguageListConfigDTO implements ConfigValueDTO {
    @NotEmpty(message = "At least one language must be selected")
    @Size(max = 4, message = "Maximum 4 languages are allowed")
    private List<@NotNull @Valid LanguageConfigDTO> languages;
}
