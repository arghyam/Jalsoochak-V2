package com.example.tenant.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationEvent {
    private String eventType;
    private int escalationLevel;
    private String officerPhone;
    private String officerName;
    private List<OperatorEscalationDetail> operators;
}
