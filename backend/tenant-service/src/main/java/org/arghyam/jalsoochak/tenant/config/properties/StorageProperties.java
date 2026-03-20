package org.arghyam.jalsoochak.tenant.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for object storage.
 * Bound from the {@code storage.*} namespace in application.yml.
 *
 * <p>Setting {@code storage.endpoint} to a non-blank URL activates path-style
 * access and endpoint override, which is required for MinIO, Cloudflare R2,
 * DigitalOcean Spaces, and other S3-compatible providers. Leave it blank to
 * use real AWS S3 with the default regional endpoint resolution.
 */
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {

    /** Storage provider key. Currently only {@code s3} is supported. */
    private String provider = "s3";

    /**
     * Custom endpoint URL. Set to MinIO/R2/etc. URL for non-AWS providers.
     * Leave blank for real AWS S3.
     */
    private String endpoint;

    /** AWS region (or a dummy value like {@code us-east-1} for MinIO). */
    private String region = "ap-south-1";

    /** Access key / access key ID. */
    private String accessKey;

    /** Secret key / secret access key. */
    private String secretKey;

    /** Bucket name for tenant assets. */
    private String bucket = "tenant-assets";

}
