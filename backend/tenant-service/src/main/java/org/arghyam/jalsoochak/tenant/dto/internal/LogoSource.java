package org.arghyam.jalsoochak.tenant.dto.internal;

import org.springframework.web.multipart.MultipartFile;

/**
 * Represents the two mutually exclusive ways a tenant logo can be set:
 * {@link FileSource} for a binary file upload to internal storage, or
 * {@link UrlSource} for an external URL stored as-is.
 *
 * Use {@link #from(MultipartFile, String)} to construct an instance — it enforces that
 * exactly one of the two inputs is present. The service always receives exactly one subtype.
 */
public sealed interface LogoSource permits LogoSource.FileSource, LogoSource.UrlSource {

    /** Logo provided as a binary file for upload to internal object storage. */
    record FileSource(MultipartFile file) implements LogoSource {}

    /** Logo provided as an external URL (http/https). */
    record UrlSource(String url) implements LogoSource {
        public UrlSource {
            try {
                java.net.URI uri = new java.net.URI(url);
                String scheme = uri.getScheme();
                if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                    throw new IllegalArgumentException("URL must use http or https scheme: " + url);
                }
                if (uri.getHost() == null || uri.getHost().isBlank()) {
                    throw new IllegalArgumentException("URL must have a valid host: " + url);
                }
            } catch (java.net.URISyntaxException e) {
                throw new IllegalArgumentException("Invalid URL: " + url, e);
            }
        }
    }

    /**
     * Constructs a {@link LogoSource} from the two optional request parameters.
     * Exactly one of {@code file} or {@code url} must be non-empty.
     *
     * @throws IllegalArgumentException if neither or both are provided.
     */
    static LogoSource from(MultipartFile file, String url) {
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasUrl = url != null && !url.isBlank();
        if (!hasFile && !hasUrl) {
            throw new IllegalArgumentException("Either a logo file or an external URL must be provided.");
        }
        if (hasFile && hasUrl) {
            throw new IllegalArgumentException("Provide either a logo file or an external URL, not both.");
        }
        return hasFile ? new FileSource(file) : new UrlSource(url);
    }
}