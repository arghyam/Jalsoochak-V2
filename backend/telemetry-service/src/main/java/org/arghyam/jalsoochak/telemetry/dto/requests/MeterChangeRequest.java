package org.arghyam.jalsoochak.telemetry.dto.requests;

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
public class MeterChangeRequest {
    private String contactId;

    @JsonAlias({"selection", "selectedReason", "meterChangeReason", "channel", "results"})
    private String reason;
}
