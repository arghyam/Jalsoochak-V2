package org.arghyam.jalsoochak.user.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * AES-256-GCM encrypt/decrypt for staff managed passwords.
 *
 * <p>Uses a <em>separate</em> key from {@code PiiEncryptionService} so that
 * a compromise of the PII key does not expose managed passwords, and vice-versa.
 *
 * <p>Ciphertext format: {@code base64(iv[12] || ciphertext+tag)}.
 * Key source: {@code STAFF_MANAGED_PASSWORD_KEY} env var (32-byte, Base64-encoded).
 */
@Component
public class PasswordCipher {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec aesKey;
    private final SecureRandom rng = new SecureRandom();

    public PasswordCipher(@Value("${staff.managed-password-key}") String encodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "STAFF_MANAGED_PASSWORD_KEY must decode to exactly 32 bytes (256 bits)");
        }
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts {@code plaintext} with AES-256-GCM.
     *
     * @return {@code base64(iv || ciphertext+tag)}, or {@code null} if input is {@code null}
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            rng.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertextAndTag = cipher.doFinal(plaintext.getBytes(UTF_8));

            byte[] output = new byte[IV_LENGTH_BYTES + ciphertextAndTag.length];
            System.arraycopy(iv, 0, output, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertextAndTag, 0, output, IV_LENGTH_BYTES, ciphertextAndTag.length);

            return Base64.getEncoder().encodeToString(output);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    /**
     * Decrypts a value produced by {@link #encrypt}.
     *
     * @return plaintext, or {@code null} if input is {@code null}
     * @throws IllegalStateException if decryption fails (tampered data or wrong key)
     */
    public String decrypt(String encoded) {
        if (encoded == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH_BYTES);
            byte[] ciphertextAndTag = Arrays.copyOfRange(decoded, IV_LENGTH_BYTES, decoded.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(ciphertextAndTag), UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM decryption failed", e);
        }
    }
}
