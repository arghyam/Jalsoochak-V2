package org.arghyam.jalsoochak.tenant.dto.internal;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public final class StateITSystemConfigDTO implements ConfigValueDTO {

    @NotBlank(message = "API endpoint is required")
    private String apiEndpoint;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Organization code is required")
    private String organizationCode;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @ToString.Exclude
    private Map<String, Object> additionalSettings = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalSettings() {
        if (additionalSettings == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> filtered = new HashMap<>();
        additionalSettings.forEach((k, v) -> {
            if (!KNOWN_PROPERTIES.contains(k)) {
                filtered.put(k, v);
            }
       });
       return Collections.unmodifiableMap(filtered);
    }

    private static final Set<String> KNOWN_PROPERTIES = Set.of(
            "apiEndpoint", "username", "password", "organizationCode");

    @JsonAnySetter
    public void addAdditionalSetting(String key, Object value) {
        if (KNOWN_PROPERTIES.contains(key)) {
            throw new IllegalArgumentException(
                    "'" + key + "' is a declared field and cannot be set via additionalSettings");
        }
        if (additionalSettings == null) {
            additionalSettings = new HashMap<>();
        }
        additionalSettings.put(key, value);
    }
}
