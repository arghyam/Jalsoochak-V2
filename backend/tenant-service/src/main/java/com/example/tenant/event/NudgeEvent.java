package com.example.tenant.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NudgeEvent {
    private String eventType;
    private String recipientPhone;
    private String operatorName;
    private String schemeId;
    private int tenantId;
    private int languageId;
}
