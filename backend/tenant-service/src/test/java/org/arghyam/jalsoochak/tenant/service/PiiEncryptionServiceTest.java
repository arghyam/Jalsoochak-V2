package org.arghyam.jalsoochak.tenant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PiiEncryptionService - Unit Tests")
class PiiEncryptionServiceTest {

    // 32 bytes of zeros, base64-encoded — safe test key material
    private static final String TEST_AES_KEY  = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final String TEST_HMAC_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private PiiEncryptionService pii;

    @BeforeEach
    void setUp() {
        pii = new PiiEncryptionService(TEST_AES_KEY, TEST_HMAC_KEY);
    }

    // ── encrypt ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("encrypt(null) returns null")
    void encrypt_null_returnsNull() {
        assertNull(pii.encrypt(null));
    }

    @Test
    @DisplayName("encrypt trims leading/trailing whitespace before encrypting")
    void encrypt_trimsWhitespace() {
        String ciphertext = pii.encrypt("  hello  ");
        assertEquals("hello", pii.decrypt(ciphertext));
    }

    @Test
    @DisplayName("encrypt produces distinct ciphertexts for the same plaintext (random IV)")
    void encrypt_producesDistinctCiphertexts() {
        String c1 = pii.encrypt("test");
        String c2 = pii.encrypt("test");
        assertNotEquals(c1, c2, "Two encryptions of the same plaintext should differ");
    }

    // ── decrypt ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("decrypt(null) returns null")
    void decrypt_null_returnsNull() {
        assertNull(pii.decrypt(null));
    }

    @Test
    @DisplayName("round-trip encrypt/decrypt returns original plaintext")
    void decrypt_roundTrip() {
        String plaintext = "91XXXXXXXXXX";
        assertEquals(plaintext, pii.decrypt(pii.encrypt(plaintext)));
    }

    @Test
    @DisplayName("decrypt throws IllegalStateException for ciphertext shorter than IV length")
    void decrypt_tooShort_throwsIllegalState() {
        String tooShort = Base64.getEncoder().encodeToString(new byte[5]);
        assertThrows(IllegalStateException.class, () -> pii.decrypt(tooShort));
    }

    @Test
    @DisplayName("decrypt throws IllegalStateException for invalid base64 input")
    void decrypt_invalidBase64_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> pii.decrypt("!!!invalid-base64!!!"));
    }

    @Test
    @DisplayName("decrypt throws IllegalStateException for tampered ciphertext (auth tag mismatch)")
    void decrypt_tamperedCiphertext_throwsIllegalState() {
        String good = pii.encrypt("original");
        byte[] bytes = Base64.getDecoder().decode(good);
        bytes[bytes.length / 2] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(bytes);
        assertThrows(IllegalStateException.class, () -> pii.decrypt(tampered));
    }

    // ── safeDecrypt ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("safeDecrypt(null) returns null")
    void safeDecrypt_null_returnsNull() {
        assertNull(pii.safeDecrypt(null));
    }

    @Test
    @DisplayName("safeDecrypt decrypts a properly encrypted value")
    void safeDecrypt_validCiphertext_returnsPlaintext() {
        String plaintext = "91XXXXXXXXXX";
        assertEquals(plaintext, pii.safeDecrypt(pii.encrypt(plaintext)));
    }

    @Test
    @DisplayName("safeDecrypt returns raw value for non-Base64 input (legacy plaintext)")
    void safeDecrypt_invalidBase64_returnsRaw() {
        String legacy = "plaintext-phone";
        assertEquals(legacy, pii.safeDecrypt(legacy));
    }

    @Test
    @DisplayName("safeDecrypt returns raw value for Base64 input shorter than 28 bytes (legacy plaintext)")
    void safeDecrypt_tooShortBase64_returnsRaw() {
        // 10 bytes — valid Base64 but too short to be AES-GCM (minimum is 28 bytes)
        String short64 = Base64.getEncoder().encodeToString(new byte[10]);
        assertEquals(short64, pii.safeDecrypt(short64));
    }

    @Test
    @DisplayName("safeDecrypt throws IllegalStateException for tampered ciphertext (does not swallow real failures)")
    void safeDecrypt_tamperedCiphertext_throwsIllegalState() {
        String good = pii.encrypt("original");
        byte[] bytes = Base64.getDecoder().decode(good);
        bytes[bytes.length / 2] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(bytes);
        assertThrows(IllegalStateException.class, () -> pii.safeDecrypt(tampered));
    }

    // ── hmac ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hmac(null) returns null")
    void hmac_null_returnsNull() {
        assertNull(pii.hmac(null));
    }

    @Test
    @DisplayName("hmac returns a 64-character lowercase hex string")
    void hmac_returns64CharHex() {
        String result = pii.hmac("91XXXXXXXXXX");
        assertNotNull(result);
        assertEquals(64, result.length());
        assertTrue(result.matches("[0-9a-f]{64}"), "HMAC output should be lowercase hex");
    }

    @Test
    @DisplayName("hmac is deterministic for the same input")
    void hmac_isDeterministic() {
        assertEquals(pii.hmac("91XXXXXXXXXX"), pii.hmac("91XXXXXXXXXX"));
    }

    @Test
    @DisplayName("hmac trims whitespace before hashing")
    void hmac_trimsWhitespace() {
        assertEquals(pii.hmac("91XXXXXXXXXX"), pii.hmac("  91XXXXXXXXXX  "));
    }
}
