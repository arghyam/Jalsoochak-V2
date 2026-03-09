package org.arghyam.jalsoochak.tenant.dto.internal;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString(exclude = "apiKey")
@EqualsAndHashCode(exclude = "apiKey")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class MessageBrokerConfigDTO implements ConfigValueDTO {

    @NotBlank(message = "API URL is required")
    private String apiUrl;

    @NotBlank(message = "API key is required")
    private String apiKey;

    @NotBlank(message = "Organization ID is required")
    private String organizationId;

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
