package com.example.telemetry.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntroResponse {
    private boolean success;
    private String message;
    private String correlationId;
    private Boolean hasBfm;
    private Boolean hasElectric;
    private Boolean isBfmorIsElectrical;
    private Boolean isBfm;
    private Boolean isElectrical;
}
