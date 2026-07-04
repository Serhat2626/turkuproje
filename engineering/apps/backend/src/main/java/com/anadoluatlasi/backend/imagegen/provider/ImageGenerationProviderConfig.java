package com.anadoluatlasi.backend.imagegen.provider;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FP-007: Image generation provider configuration.
 *
 * <p>Provider seçimi ve timeout ayarları.
 * API key bu class'ta tutulmaz — environment variable üzerinden okunur.</p>
 *
 * <p>Varsayılan: mode=mock. Real provider sadece explicit config ile aktif olur.</p>
 *
 * <p>Config DTO/API response'a, log'a veya test fixture'a çıkmaz.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "imagegen.provider")
public class ImageGenerationProviderConfig {

    /**
     * Provider mode: "mock" veya "openai".
     * Default: mock — gerçek provider çağrısı yapılmaz.
     */
    private String mode = "mock";

    /**
     * Provider request timeout (ms).
     * Default: 60 saniye.
     */
    private int timeoutMs = 60000;

    /**
     * Maksimum output sayısı.
     * Default: 1.
     */
    private int maxOutputCount = 1;

    /**
     * OpenAI model adı.
     * Default: dall-e-3.
     */
    private String modelName = "dall-e-3";

    /**
     * OpenAI image size.
     * Default: 1024x1024.
     */
    private String imageSize = "1024x1024";

    /**
     * Mock provider mı aktif?
     */
    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(mode);
    }
}
