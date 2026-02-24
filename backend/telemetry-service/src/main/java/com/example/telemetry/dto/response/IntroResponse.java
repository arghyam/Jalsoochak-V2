package com.example.telemetry.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntroResponse {
    private boolean success;
    private String message;
    private String correlationId;
}
