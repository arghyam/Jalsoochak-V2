package com.example.analytics.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEvent {

    private String eventType;
    private Integer userId;
    private Integer tenantId;
    private String email;
    private Integer userType;
}
