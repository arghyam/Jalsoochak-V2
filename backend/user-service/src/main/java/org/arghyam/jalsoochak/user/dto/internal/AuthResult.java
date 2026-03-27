package org.arghyam.jalsoochak.user.dto.internal;

import org.arghyam.jalsoochak.user.dto.response.TokenResponseDTO;

/**
 * Internal carrier for auth operation results.
 * The controller uses this to set the httpOnly refresh-token cookie
 * without leaking HTTP concerns into the service layer.
 */
public record AuthResult(
        TokenResponseDTO tokenResponse,
        String refreshToken,
        int refreshExpiresIn
) {}
