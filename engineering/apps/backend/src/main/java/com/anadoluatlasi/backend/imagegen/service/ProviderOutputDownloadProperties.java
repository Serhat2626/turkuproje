package com.anadoluatlasi.backend.imagegen.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * FP-010 Task 02: Provider output download client configuration.
 *
 * <p>Controls HTTP download behavior for provider output retrieval.
 * Disabled by default — uses MockProviderOutputRetrievalClient.</p>
 *
 * <p>No secrets in this config — provider URL is transient method parameter.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "imagegen.provider.download")
public class ProviderOutputDownloadProperties {

    /** Whether real HTTP download is enabled. Default: false (uses mock). */
    private boolean enabled = false;

    /** Connect timeout in milliseconds. */
    private int connectTimeoutMs = 5000;

    /** Read timeout in milliseconds. */
    private int readTimeoutMs = 30000;

    /** Maximum download size in bytes. Default: 20MB. */
    private long maxBytes = 20 * 1024 * 1024;

    /** Maximum number of redirects to follow. 0 = no redirects. */
    private int maxRedirects = 3;

    /** Allowed response content types. */
    private Set<String> allowedContentTypes = Set.of(
            "image/png", "image/jpeg", "image/webp"
    );
}
