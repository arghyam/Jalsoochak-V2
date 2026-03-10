package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.internal.AuthResult;
import org.arghyam.jalsoochak.user.dto.request.ActivateAccountRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.ForgotPasswordRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.LoginRequestDTO;
import org.arghyam.jalsoochak.user.dto.request.ResetPasswordRequestDTO;
import org.arghyam.jalsoochak.user.dto.response.InviteInfoResponseDTO;

public interface AuthService {

    AuthResult login(LoginRequestDTO request);

    AuthResult refreshToken(String refreshToken);

    boolean logout(String refreshToken);

    InviteInfoResponseDTO getInviteInfo(String rawInviteJwt);

    AuthResult activateAccount(ActivateAccountRequestDTO request);

    void forgotPassword(ForgotPasswordRequestDTO request);

    void resetPassword(ResetPasswordRequestDTO request);
}
