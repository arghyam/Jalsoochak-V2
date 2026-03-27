package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual reason item in reason lists.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class ReasonItemDTO {
    @NotBlank(message = "Reason ID is required")
    private String id;
    
    @NotBlank(message = "Reason name is required")
    private String name;
    
    @NotNull(message = "Sequence order is required")
    @Positive(message = "Sequence order must be positive")
    private Integer sequenceOrder;
    
    @NotNull(message = "isDefault flag is required")
    private Boolean isDefault;
    
    @NotNull(message = "editable flag is required")
    private Boolean editable;
}
