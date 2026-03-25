package org.arghyam.jalsoochak.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.arghyam.jalsoochak.user.validation.ValidOtp;

/**
 * Request body for {@code POST /api/v1/auth/staff/verify-otp}.
 */
@Getter
@Setter
public class StaffOtpVerifyDTO {

    /**
     * Phone number in E.164 format without the leading {@code +}.
     * Must match the number used in the preceding {@code request-otp} call.
     */
    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[1-9]\\d{6,14}$",
             message = "phoneNumber must be digits only, no leading zero, 7–15 digits total")
    private String phoneNumber;

    /** State code (e.g. {@code MP}). Must match the value used in {@code request-otp}. */
    @NotBlank(message = "tenantCode is required")
    private String tenantCode;

    /** The OTP received via WhatsApp or SMS. Length and digit-only validated via {@link ValidOtp}. */
    @NotBlank(message = "otp is required")
    @ValidOtp
    private String otp;
}
