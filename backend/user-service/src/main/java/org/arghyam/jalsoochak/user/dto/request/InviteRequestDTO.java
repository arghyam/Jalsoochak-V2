package org.arghyam.jalsoochak.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteRequestDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "SUPER_USER|STATE_ADMIN", message = "Role must be SUPER_USER or STATE_ADMIN")
    private String role;

    @Pattern(regexp = "^[A-Z]{2,4}$", message = "Tenant code must be 2-4 uppercase letters")
    private String tenantCode;
}
