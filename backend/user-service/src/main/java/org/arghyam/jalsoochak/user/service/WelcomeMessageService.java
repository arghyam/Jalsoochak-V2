package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.request.WelcomeMessageRequestDTO;
import org.arghyam.jalsoochak.user.dto.response.WelcomeMessageResponseDTO;
import org.springframework.security.core.Authentication;

public interface WelcomeMessageService {

    WelcomeMessageResponseDTO sendWelcomeMessages(
            String tenantCode,
            WelcomeMessageRequestDTO request,
            Authentication authentication
    );
}
