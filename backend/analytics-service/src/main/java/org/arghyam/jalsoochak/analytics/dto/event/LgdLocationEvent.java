package org.arghyam.jalsoochak.analytics.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LgdLocationEvent {

    private String eventType;
    private Integer lgdId;
    private Integer tenantId;
    private String lgdCode;
    private String lgdCName;
    private String title;
    private Integer lgdLevel;
    private Integer level1LgdId;
    private Integer level2LgdId;
    private Integer level3LgdId;
    private Integer level4LgdId;
    private Integer level5LgdId;
    private Integer level6LgdId;
    private String geom;
}
