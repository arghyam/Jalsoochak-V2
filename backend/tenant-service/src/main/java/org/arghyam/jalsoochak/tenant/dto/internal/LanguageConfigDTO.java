package org.arghyam.jalsoochak.tenant.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageConfigDTO {
    private String language;
    private Integer preference;
}
