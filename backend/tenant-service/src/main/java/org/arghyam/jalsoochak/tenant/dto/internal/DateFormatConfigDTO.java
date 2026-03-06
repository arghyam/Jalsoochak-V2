package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arghyam.jalsoochak.tenant.validation.ValidTimeZone;

/**
 * DTO for date and time format configuration.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class DateFormatConfigDTO implements ConfigValueDTO {
    @NotBlank(message = "Date format required (e.g., DD-MON-YYYY or DD/MM/YYYY)")
    private String dateFormat;
    
    @NotBlank(message = "Time format required")
    @Pattern(regexp = "24H|12H", message = "Time format must be '24H' or '12H'")
    private String timeFormat;
    
    @NotBlank(message = "Timezone required (e.g., Asia/Kolkata)")
    @ValidTimeZone
    private String timezone;
}
