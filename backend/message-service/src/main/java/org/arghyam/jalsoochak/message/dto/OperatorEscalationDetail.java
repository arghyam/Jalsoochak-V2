package org.arghyam.jalsoochak.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorEscalationDetail {
    private String name;
    private String phoneNumber;
    private String schemeName;
    private String schemeId;
    private String soName;
    private int consecutiveDaysMissed;
    private String lastRecordedBfmDate;
}
