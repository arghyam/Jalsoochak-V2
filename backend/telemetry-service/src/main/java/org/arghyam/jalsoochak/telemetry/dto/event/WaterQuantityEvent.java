package org.arghyam.jalsoochak.telemetry.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterQuantityEvent {

    private String eventType;
    private Integer tenantId;
    private Integer schemeId;
    private Integer userId;
    private Integer waterQuantity;
    private Integer submissionStatus;
    private String outageReason;
    private String nonSubmissionReason;
    private String date;
}
