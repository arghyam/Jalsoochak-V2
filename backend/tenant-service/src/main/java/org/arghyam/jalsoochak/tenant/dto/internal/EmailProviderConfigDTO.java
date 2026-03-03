package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Email Provider SMTP connection settings.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class EmailProviderConfigDTO implements ConfigValueDTO {
    @NotBlank(message = "SMTP host is required")
    private String host;
    
    @NotNull(message = "SMTP port is required")
    @Min(1)
    @Max(65535)
    private Integer port;
    
    @NotBlank(message = "SMTP username is required")
    @Email(message = "Username must be a valid email")
    private String username;
    
    @NotBlank(message = "SMTP password is required")
    private String password;
    
    private Boolean useTls;
    
    @Positive(message = "Connection timeout must be positive")
    private Long connectionTimeoutMs;
    
    @Positive(message = "Read timeout must be positive")
    private Long readTimeoutMs;
}
