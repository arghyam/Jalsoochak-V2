package org.arghyam.jalsoochak.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Shared helper for reading and decrypting PII fields stored in JSON token metadata.
 * Falls back to the raw value for legacy tokens that stored names as plaintext.
 */
@Component
@RequiredArgsConstructor
public class MetadataDecryptionHelper {

    private final ObjectMapper objectMapper;
    private final PiiEncryptionService pii;

    /**
     * Reads {@code key} from the JSON string {@code json}, then decrypts the value if it
     * was stored encrypted. Returns the raw value for legacy plaintext tokens.
     */
    public String parseAndDecrypt(String json, String key) {
        String raw = parse(json, key);
        if (raw == null) return null;
        return pii.safeDecrypt(raw);
    }

    private String parse(String json, String key) {
        if (json == null) return null;
        try {
            return objectMapper.readTree(json).path(key).asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
