package org.arghyam.jalsoochak.scheme.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtTokenValidator {

    private final String issuerUri;
    private final String keycloakPublicKey;

    private volatile JwtDecoder decoder;

    public JwtTokenValidator(
            @Value("${KEYCLOAK_ISSUER_URI:}") String issuerUri,
            @Value("${KEYCLOAK_PUBLIC_KEY:}") String keycloakPublicKey
    ) {
        this.issuerUri = issuerUri == null ? "" : issuerUri.trim();
        this.keycloakPublicKey = keycloakPublicKey == null ? "" : keycloakPublicKey.trim();
    }

    public Jwt decodeAndValidate(String token) throws JwtException {
        return getDecoder().decode(token);
    }

    private JwtDecoder getDecoder() {
        JwtDecoder local = decoder;
        if (local != null) {
            return local;
        }

        synchronized (this) {
            if (decoder != null) {
                return decoder;
            }
            if (keycloakPublicKey.isBlank()) {
                throw new IllegalStateException("KEYCLOAK_PUBLIC_KEY is not configured for scheme-service");
            }

            RSAPublicKey publicKey = readRsaPublicKey(keycloakPublicKey);
            NimbusJwtDecoder nimbus = NimbusJwtDecoder.withPublicKey(publicKey).build();

            // Always validate exp/nbf; validate issuer if configured.
            OAuth2TokenValidator<Jwt> validator = issuerUri.isBlank()
                    ? new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator())
                    : JwtValidators.createDefaultWithIssuer(issuerUri);
            nimbus.setJwtValidator(validator);

            decoder = nimbus;
            return nimbus;
        }
    }

    private static RSAPublicKey readRsaPublicKey(String value) {
        String cleaned = value
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse KEYCLOAK_PUBLIC_KEY as RSA public key", e);
        }
    }
}

