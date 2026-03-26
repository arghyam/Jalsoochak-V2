package org.arghyam.jalsoochak.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WelcomeMessageRequestDTO {

    @NotEmpty(message = "At least one role is required")
    private List<String> roles;

    @NotBlank(message = "Template type is required")
    private String type;

    /** Optional ISO-8601 timestamp or yyyy-MM-dd date (UTC) to filter newly onboarded users. */
    private String onboardedAfter;

    /** Optional ISO-8601 timestamp or yyyy-MM-dd date (UTC) to filter newly onboarded users. */
    private String onboardedBefore;
}
