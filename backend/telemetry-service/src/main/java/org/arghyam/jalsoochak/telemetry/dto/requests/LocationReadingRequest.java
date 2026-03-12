package org.arghyam.jalsoochak.telemetry.dto.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationReadingRequest {

    @JsonAlias({"organization_id", "organizationId"})
    private Integer organizationId;

    @NotNull
    @JsonAlias({"lat", "latitude"})
    private BigDecimal latitude;

    @NotNull
    @JsonAlias({"lng", "lon", "long", "longitude"})
    private BigDecimal longitude;

    @Valid
    @NotNull
    private Contact contact;

    @JsonIgnore
    public String resolveContactId() {
        return contact != null ? contact.phone : null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        @NotBlank
        private String phone;

        private String name;

        private Long id;
    }
}
