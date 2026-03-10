package org.arghyam.jalsoochak.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponseDTO {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("person_id")
    private Long personId;

    @JsonProperty("tenant_id")
    private Integer tenantId;

    @JsonProperty("tenant_code")
    private String tenantCode;

    @JsonProperty("user_role")
    private String role;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("name")
    private String name;
}