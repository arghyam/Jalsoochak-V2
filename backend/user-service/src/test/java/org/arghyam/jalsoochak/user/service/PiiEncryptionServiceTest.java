package org.arghyam.jalsoochak.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        // Different IVs → different ciphertexts
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
        // 5 bytes — shorter than the 12-byte IV
        String tooShort = Base64.getEncoder().encodeToString(new byte[5]);
        assertThrows(IllegalStateException.class, () -> pii.decrypt(tooShort));
    }

    @Test
    @DisplayName("decrypt throws IllegalStateException for invalid base64 input")
    void decrypt_invalidBase64_throwsIllegalState() {
        // '!' is not a valid base64 character → IllegalArgumentException wrapped as IllegalStateException
        assertThrows(IllegalStateException.class, () -> pii.decrypt("!!!invalid-base64!!!"));
    }

    @Test
    @DisplayName("decrypt throws IllegalStateException for tampered ciphertext (auth tag mismatch)")
    void decrypt_tamperedCiphertext_throwsIllegalState() {
        String good = pii.encrypt("original");
        // Flip a byte in the middle of the ciphertext
        byte[] bytes = Base64.getDecoder().decode(good);
        bytes[bytes.length / 2] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(bytes);
        assertThrows(IllegalStateException.class, () -> pii.decrypt(tampered));
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
