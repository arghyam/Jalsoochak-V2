package org.arghyam.jalsoochak.tenant.dto.internal;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class StateITSystemConfigDTO implements ConfigValueDTO {

    @NotBlank(message = "API endpoint is required")
    private String apiEndpoint;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Organization code is required")
    private String organizationCode;

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, Object> additionalSettings = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalSettings() {
        return additionalSettings;
    }

    @JsonAnySetter
    public void addAdditionalSetting(String key, Object value) {
        additionalSettings.put(key, value);
    }
}
