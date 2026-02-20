package com.example.user.service;

import com.example.user.dto.request.InviteRequest;
import com.example.user.dto.request.LoginRequest;
import com.example.user.dto.request.RegisterRequest;
import com.example.user.dto.response.TokenResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {

    String inviteUser(InviteRequest inviteRequest);

    void completeProfile(RegisterRequest registerRequest);

    TokenResponse login(LoginRequest loginRequest, String tenantCode);

    TokenResponse refreshToken(String refreshToken);

    boolean logout(String refreshToken);

//    Map<String, Object> bulkInviteUsers(MultipartFile file, String tenantCode);
}
