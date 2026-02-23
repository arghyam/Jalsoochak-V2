package com.example.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequest {
    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
