package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for simple configuration values.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class SimpleConfigValueDTO implements ConfigValueDTO {
    @NotBlank(message = "Value cannot be blank")
    private String value;
}
