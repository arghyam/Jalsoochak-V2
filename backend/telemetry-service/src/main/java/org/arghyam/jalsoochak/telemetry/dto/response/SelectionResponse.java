package org.arghyam.jalsoochak.telemetry.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SelectionResponse {
    private boolean success;
    private String selected;
    private String message;
}
