package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.request.InviteRequest;
import org.arghyam.jalsoochak.user.dto.request.LoginRequest;
import org.arghyam.jalsoochak.user.dto.request.RegisterRequest;
import org.arghyam.jalsoochak.user.dto.response.TokenResponse;

public interface UserService {

    void inviteUser(InviteRequest inviteRequest);

    void completeProfile(RegisterRequest registerRequest);

    TokenResponse login(LoginRequest loginRequest, String tenantCode);

    TokenResponse refreshToken(String refreshToken);

    boolean logout(String refreshToken);

//    Map<String, Object> bulkInviteUsers(MultipartFile file, String tenantCode);
}
