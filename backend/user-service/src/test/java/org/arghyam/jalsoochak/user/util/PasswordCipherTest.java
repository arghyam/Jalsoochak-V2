package org.arghyam.jalsoochak.user.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordCipher")
class PasswordCipherTest {

    // 32 random bytes, base64-encoded — test-only key
    private static final String TEST_KEY =
            Base64.getEncoder().encodeToString(new byte[32]);

    PasswordCipher cipher;

    @BeforeEach
    void setUp() {
        cipher = new PasswordCipher(TEST_KEY);
    }

    @Test
    @DisplayName("encrypts and decrypts round-trip correctly")
    void encryptDecryptRoundTrip() {
        String plaintext = "super-secret-managed-password-abc123";
        String encrypted = cipher.encrypt(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("produces different ciphertext for the same plaintext (random IV)")
    void differentCiphertextEachTime() {
        String p = "same-plaintext";
        assertThat(cipher.encrypt(p)).isNotEqualTo(cipher.encrypt(p));
    }

    @Test
    @DisplayName("returns null for null input")
    void handlesNullInput() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("throws IllegalStateException on tampered ciphertext")
    void throwsOnTamperedCiphertext() {
        String encrypted = cipher.encrypt("valid");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";
        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when key is not 32 bytes")
    void throwsOnWrongKeySize() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new PasswordCipher(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("throws IllegalStateException when key is not valid Base64")
    void throwsOnInvalidBase64Key() {
        assertThatThrownBy(() -> new PasswordCipher("not-valid-base64!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    @DisplayName("throws IllegalStateException when ciphertext is not valid Base64")
    void throwsOnInvalidBase64Ciphertext() {
        assertThatThrownBy(() -> cipher.decrypt("not-valid-base64!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    @DisplayName("throws IllegalStateException when decoded ciphertext is too short")
    void throwsOnTooShortCiphertext() {
        // 27 bytes — one byte below the 28-byte minimum (12 IV + 16 GCM tag)
        String tooShort = Base64.getEncoder().encodeToString(new byte[27]);
        assertThatThrownBy(() -> cipher.decrypt(tooShort))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }
}
