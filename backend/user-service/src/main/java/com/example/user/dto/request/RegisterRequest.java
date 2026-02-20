package com.example.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[A-Za-z]+$", message = "First name can contain only alphabets")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[A-Za-z]+$", message = "Last name can contain only alphabets")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
//    @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
//    @Pattern(regexp = "\\d+", message = "Phone number should contain only digits")
    private String phoneNumber;

    @NotBlank(message = "Person type is required")
    private String personType;

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Tenant ID is required e.g assam")
    private String tenantId;
}
