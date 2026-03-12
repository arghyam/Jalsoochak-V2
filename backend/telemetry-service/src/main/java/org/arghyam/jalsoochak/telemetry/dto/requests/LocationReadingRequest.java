package org.arghyam.jalsoochak.telemetry.dto.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @NotBlank
    private String contactId;

    @NotNull
    @JsonAlias({"lat", "latitude"})
    private BigDecimal latitude;

    @NotNull
    @JsonAlias({"lng", "lon", "long", "longitude"})
    private BigDecimal longitude;
}
