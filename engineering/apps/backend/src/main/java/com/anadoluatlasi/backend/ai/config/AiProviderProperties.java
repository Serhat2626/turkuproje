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
     * Video üretim provider: {@code none} veya {@code kling}.
     * Varsayılan: {@code none} — aktif değil
     */
    private String videoProvider = "none";

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
     * Flux Pro v1.1 — son derece yüksek kaliteli görseller.
     */
    private Fal fal = new Fal();

    /**
     * Kling AI Image-to-Video provider ayarları.
     * Fal.ai üzerinden erişilir — aynı API key kullanılır.
     */
    private Kling kling = new Kling();

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

        /** HTTP istek timeout (saniye) — PLANNING_BUNDLE uzun sürer */
        private int timeoutSeconds = 180;

        /** Maksimum output token sayısı — 35 sahne storyboard + hikâye + senaryo + karakter çizelgesi */
        private int maxOutputTokens = 32000;

        /** Yaratıcılık parametresi (0.0–2.0) */
        private double temperature = 0.4;
    }

    /**
     * ElevenLabs TTS provider ayarları.
     * Duygusal, sinematik Türkçe seslendirme için optimize edilmiş.
     */
    @Data
    public static class ElevenLabs {

        /** ElevenLabs API key */
        private String apiKey;

        /** Voice ID — Türkçe hikâye anlatımı için duygusal ses
         *  Fatih: fZEjvS1BvpajAoOmWcEO (erkek, orta yaş, hikâyeci)
         *  Ayça D: 5RqXmIU9ikjifeWoXHMG (kadın, sakin, samimi)
         *  Asım Mirza: mEf7WY0hvNO6KqdAkUeA (erkek, derin, etkileyici)
         */
        private String voiceId = "fZEjvS1BvpajAoOmWcEO";  // "Fatih" - hikâye anlatıcı, duygusal

        /** Model ID — v3 daha iyi Türkçe ve duygusal tonlama */
        private String modelId = "eleven_multilingual_v2";

        /** Stability (0.0-1.0) — DÜŞÜK = daha duygusal, doğal iniş-çıkışlar */
        private double stability = 0.35;

        /** Similarity boost (0.0-1.0) — sesin orijinal karakterini korur */
        private double similarityBoost = 0.80;

        /** Style (0.0-1.0) — YÜKSEK = daha dramatik, ifadeli anlatım */
        private double style = 0.65;

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

    /**
     * Kling AI Image-to-Video provider ayarları.
     * Fal.ai üzerinden erişilir — aynı FAL_API_KEY kullanılır.
     */
    @Data
    public static class Kling {

        /** Kling model endpoint — standard veya pro */
        private String model = "fal-ai/kling-video/v2/master/image-to-video";

        /** Video süresi: "5" veya "10" saniye */
        private String duration = "5";

        /** Hareket prompt'u için ek context */
        private String motionPromptSuffix = "Gentle, subtle natural movement. Cinematic camera. Photorealistic.";
    }
}
