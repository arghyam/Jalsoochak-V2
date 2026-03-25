package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.internal.AuthResult;
import org.arghyam.jalsoochak.user.dto.request.StaffOtpRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.StaffOtpVerifyDTO;

public interface StaffAuthService {

    /**
     * Requests an OTP for a staff user.
     * Always returns without error regardless of whether the phone is registered
     * (OWASP anti-enumeration).
     */
    void requestOtp(StaffOtpRequestDTO request);

    /**
     * Verifies the OTP and returns a Keycloak access token on success.
     *
     * @throws org.arghyam.jalsoochak.user.exceptions.BadRequestException      if OTP is invalid/expired
     * @throws org.arghyam.jalsoochak.user.exceptions.AccountDeactivatedException if account is inactive
     */
    AuthResult verifyOtp(StaffOtpVerifyDTO request);
}
