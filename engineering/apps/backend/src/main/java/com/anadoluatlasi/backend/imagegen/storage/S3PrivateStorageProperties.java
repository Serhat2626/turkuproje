package com.anadoluatlasi.backend.imagegen.storage;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FP-009 Task 08: S3-compatible private storage configuration.
 *
 * <p>Configuration for S3/MinIO private image storage.
 * Disabled by default — requires explicit opt-in.</p>
 *
 * <p>Security rules:</p>
 * <ul>
 *   <li>accessKey and secretKey are excluded from toString()/logging via @ToString.Exclude</li>
 *   <li>Credentials are never exposed in DTO/API/error messages</li>
 *   <li>Config values are never committed to source control</li>
 *   <li>Use environment variables or external secret management</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "imagegen.storage.s3")
public class S3PrivateStorageProperties {

    /** Whether S3 storage is enabled. Default: false (uses InMemory mock). */
    private boolean enabled = false;

    /** S3 endpoint URL (e.g. "http://localhost:9000" for MinIO). */
    private String endpoint = "";

    /** AWS region or MinIO region. */
    private String region = "us-east-1";

    /** Target bucket name. Internal-only — never exposed to admin/user. */
    private String bucket = "";

    /** Object key prefix for image storage (e.g. "imagegen/"). */
    private String pathPrefix = "imagegen/";

    /** S3 access key — NEVER logged, serialized, or exposed. */
    @ToString.Exclude
    private String accessKey = "";

    /** S3 secret key — NEVER logged, serialized, or exposed. */
    @ToString.Exclude
    private String secretKey = "";
}
