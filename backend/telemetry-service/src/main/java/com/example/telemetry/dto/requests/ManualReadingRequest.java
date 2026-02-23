package com.example.telemetry.dto.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManualReadingRequest {
    private String contactId;
    private String manualReading;

    @JsonAlias({"selection", "selectedReason", "meterChangeReason", "reason"})
    private String meterChangeReason;

    @JsonAlias({"sessionToken", "correlationId"})
    private String correlationId;
}
