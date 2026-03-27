package org.arghyam.jalsoochak.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteInfoResponseDTO {
    private String email;
    private String role;
    private String tenantName;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
