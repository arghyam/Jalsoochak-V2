package org.arghyam.jalsoochak.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "phone number is required")
    private String username;
    @NotBlank(message = "password is required")
    private String password;
}