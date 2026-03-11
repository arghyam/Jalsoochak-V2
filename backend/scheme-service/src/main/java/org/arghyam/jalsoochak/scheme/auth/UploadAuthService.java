package org.arghyam.jalsoochak.scheme.auth;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.scheme.repository.SchemeDbRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class UploadAuthService {

    private final JwtTokenValidator jwtTokenValidator;
    private final SchemeDbRepository schemeDbRepository;

    public int requireUserId(String schemaName, String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);

        Jwt jwt;
        try {
            jwt = jwtTokenValidator.decodeAndValidate(token);
        } catch (JwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        } catch (IllegalStateException ex) {
            // Misconfiguration (missing public key etc.)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            // Fallback for some Keycloak setups.
            email = jwt.getClaimAsString("preferred_username");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token missing user identity (email)");
        }

        Integer userId = schemeDbRepository.findUserIdByEmail(schemaName, email);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found for token");
        }

        return userId;
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }

        String trimmed = authorizationHeader.trim();
        if (trimmed.length() < 7 || !trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header must be: Bearer <token>");
        }

        String token = trimmed.substring(7).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        return token;
    }
}

