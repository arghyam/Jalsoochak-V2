package org.arghyam.jalsoochak.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.arghyam.jalsoochak.user.validation.ValidPassword;

@Getter
@Setter
public class ActivateAccountRequestDTO {
    @NotBlank(message = "Invite token is required")
    private String inviteToken;

    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNumber;
}
