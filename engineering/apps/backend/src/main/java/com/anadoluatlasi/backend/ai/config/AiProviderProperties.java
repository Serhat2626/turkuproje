package com.anadoluatlasi.backend.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FP-001: AI provider ayarları.
 *
 * <p>Varsayılan provider {@code mock}. Gerçek provider kullanımı için
 * {@code ai.provider=openai} ayarlanmalı ve OpenAI alt ayarları doldurulmalıdır.</p>
 *
 * <p>API key gibi hassas değerler environment variable ile verilmelidir.
 * Git'e commit edilmez.</p>
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AiProviderProperties {

    /**
     * Aktif AI provider: {@code mock} veya {@code openai}.
     * Varsayılan: {@code mock}
     */
    private String provider = "mock";

    /**
     * OpenAI provider'a özel ayarlar.
     */
    private OpenAi openai = new OpenAi();

    /**
     * OpenAI provider ayarları.
     */
    @Data
    public static class OpenAi {

        /** OpenAI API key — environment variable ile sağlanmalıdır */
        private String apiKey;

        /** Kullanılacak model */
        private String model = "gpt-4o-mini";

        /** OpenAI API base URL */
        private String baseUrl = "https://api.openai.com/v1";

        /** HTTP istek timeout (saniye) */
        private int timeoutSeconds = 60;

        /** Maksimum output token sayısı */
        private int maxOutputTokens = 2000;

        /** Yaratıcılık parametresi (0.0–2.0) */
        private double temperature = 0.4;
    }
}
