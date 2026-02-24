package org.arghyam.jalsoochak.analytics.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantEvent {

    private String eventType;
    private Integer tenantId;
    private String stateCode;
    private String title;
    private String countryCode;
    private Integer status;
}
