package org.arghyam.jalsoochak.tenant.dto.internal;

import java.io.InputStream;

/**
 * Represents the two possible outcomes when resolving a tenant logo.
 *
 * <ul>
 *   <li>{@link Managed} — logo is stored in internal object storage; the caller
 *       receives a raw byte stream and its content type.</li>
 *   <li>{@link External} — logo URL was set externally via configuration; the
 *       caller should redirect the client to that URL.</li>
 * </ul>
 */
public sealed interface TenantLogoResult
        permits TenantLogoResult.Managed, TenantLogoResult.External {

    /**
     * Logo is owned by internal object storage.
     *
     * @param stream      raw byte stream — caller must close after use
     * @param contentType MIME type, e.g. {@code image/png}
     */
    record Managed(InputStream stream, String contentType) implements TenantLogoResult {}

    /**
     * Logo is hosted externally; redirect the client to this URL.
     *
     * @param redirectUrl fully-qualified external URL
     */
    record External(String redirectUrl) implements TenantLogoResult {}
}
