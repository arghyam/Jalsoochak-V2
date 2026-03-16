package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for water supply threshold configuration.
 * Defines percentage deviation thresholds relative to the Water Norm
 * used to classify daily supply as undersupply or oversupply.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class WaterSupplyThresholdConfigDTO implements ConfigValueDTO {

    /**
     * Percentage below the Water Norm at which supply is flagged as undersupply.
     * E.g., 20.0 means supply below 80% of the norm is undersupply.
     */
    @NotNull(message = "Undersupply threshold percent cannot be null")
    @DecimalMin(value = "0.0", message = "Undersupply threshold percent must be >= 0")
    @DecimalMax(value = "100.0", message = "Undersupply threshold percent must be <= 100")
    private Double undersupplyThresholdPercent;

    /**
     * Percentage above the Water Norm at which supply is flagged as oversupply.
     * E.g., 20.0 means supply above 120% of the norm is oversupply.
     */
    @NotNull(message = "Oversupply threshold percent cannot be null")
    @DecimalMin(value = "0.0", message = "Oversupply threshold percent must be >= 0")
    @DecimalMax(value = "1000.0", message = "Oversupply threshold percent must be <= 1000")
    private Double oversupplyThresholdPercent;
}
