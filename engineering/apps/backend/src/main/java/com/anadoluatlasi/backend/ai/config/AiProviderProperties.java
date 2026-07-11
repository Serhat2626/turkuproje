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
     * TTS (Text-to-Speech) provider: {@code openai} veya {@code elevenlabs}.
     * Varsayılan: {@code openai}
     */
    private String ttsProvider = "openai";

    /**
     * Görsel üretim provider: {@code openai} veya {@code fal}.
     * Varsayılan: {@code openai} (gpt-image-1)
     */
    private String imageProvider = "openai";

    /**
     * OpenAI provider'a özel ayarlar.
     */
    private OpenAi openai = new OpenAi();

    /**
     * ElevenLabs TTS provider ayarları.
     */
    private ElevenLabs elevenlabs = new ElevenLabs();

    /**
     * Fal.ai görsel üretim provider ayarları.
     */
    private Fal fal = new Fal();

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

    /**
     * ElevenLabs TTS provider ayarları.
     */
    @Data
    public static class ElevenLabs {

        /** ElevenLabs API key */
        private String apiKey;

        /** Voice ID — Türkçe için uygun ses */
        private String voiceId = "EXAVITQu4vr4xnSDxMaL";  // "Sarah" - multilingual, sıcak

        /** Model ID */
        private String modelId = "eleven_multilingual_v2";

        /** Stability (0.0-1.0) — düşük=daha ifadeli, yüksek=daha kararlı */
        private double stability = 0.5;

        /** Similarity boost (0.0-1.0) */
        private double similarityBoost = 0.75;

        /** Style (0.0-1.0) — anlatım stili yoğunluğu */
        private double style = 0.4;

        /** ElevenLabs API base URL */
        private String baseUrl = "https://api.elevenlabs.io/v1";
    }

    /**
     * Fal.ai görsel üretim provider ayarları.
     * Flux Pro v1.1 — son derece yüksek kaliteli görseller.
     */
    @Data
    public static class Fal {

        /** Fal.ai API key */
        private String apiKey;

        /** Model endpoint */
        private String model = "fal-ai/flux-pro/v1.1";

        /** Fal.ai API base URL */
        private String baseUrl = "https://fal.run";

        /** Görsel boyutu */
        private String imageSize = "landscape_16_9";

        /** Inference steps (kalite vs hız dengesi) */
        private int numInferenceSteps = 28;

        /** Guidance scale */
        private double guidanceScale = 3.5;
    }
}
