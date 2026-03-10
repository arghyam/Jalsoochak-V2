package org.arghyam.jalsoochak.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequestDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
