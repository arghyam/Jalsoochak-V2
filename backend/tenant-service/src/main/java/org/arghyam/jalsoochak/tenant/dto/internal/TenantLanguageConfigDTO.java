package org.arghyam.jalsoochak.tenant.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantLanguageConfigDTO {
    private List<LanguageConfigDTO> languages;
}
