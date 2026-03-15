package org.arghyam.jalsoochak.telemetry.dto.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    /**
     * When true, indicates the meter was replaced and the submitted reading should be treated as the new baseline.
     * Nullable for backward compatibility with older payloads.
     */
    @JsonProperty("isMeterReplaced")
    private Boolean isMeterReplaced;
}
