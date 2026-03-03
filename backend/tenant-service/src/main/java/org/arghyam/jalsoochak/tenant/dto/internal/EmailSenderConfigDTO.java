package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Email Sender Identity settings.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class EmailSenderConfigDTO implements ConfigValueDTO {
    @NotBlank(message = "From email address is required")
    @Email(message = "From address must be a valid email")
    private String fromAddress;
    
    @NotBlank(message = "From name is required")
    private String fromName;
    
    @NotBlank(message = "Reply-to email address is required")
    @Email(message = "Reply-to address must be a valid email")
    private String replyToAddress;
    
    @NotBlank(message = "Reply-to name is required")
    private String replyToName;
}
