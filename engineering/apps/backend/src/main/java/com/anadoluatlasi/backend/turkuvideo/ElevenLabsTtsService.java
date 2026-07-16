package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ElevenLabs TTS (Text-to-Speech) servisi.
 * Profesyonel, doğal Türkçe seslendirme üretir.
 *
 * <p>Model: eleven_multilingual_v2 — 29 dil desteği, yüksek kalite.
 * Türkçe narration için optimize edilmiş parametreler.</p>
 *
 * <p>Fiyat: ~$0.30/1K karakter (Creator plan).
 * OpenAI TTS'e göre ~10x pahalı ama kalite farkı belirgin.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.tts-provider", havingValue = "elevenlabs")
public class ElevenLabsTtsService {

    private final RestClient restClient;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final TurkuVideoProjectRepository repository;
    private final String voiceId;
    private final String modelId;
    private final double stability;
    private final double similarityBoost;
    private final double style;

    public ElevenLabsTtsService(
            AiProviderProperties properties,
            MinioClient minioClient,
            MinioConfig minioConfig,
            TurkuVideoProjectRepository repository) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.repository = repository;

        AiProviderProperties.ElevenLabs config = properties.getElevenlabs();
        this.voiceId = config.getVoiceId();
        this.modelId = config.getModelId();
        this.stability = config.getStability();
        this.similarityBoost = config.getSimilarityBoost();
        this.style = config.getStyle();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(180)); // ElevenLabs daha yavaş olabilir

        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("xi-api-key", config.getApiKey())
                .requestFactory(factory)
                .build();

        log.info("ElevenLabs TTS servisi başlatıldı — voiceId={}, model={}", voiceId, modelId);
    }

    /**
     * Proje için tüm sahnelerin seslendirmesini üret.
     * TurkuTtsService ile aynı interface — pipeline'dan şeffaf çağrılır.
     */
    public TurkuTtsService.TurkuTtsResponse generateForProject(UUID projectId) {
        return generateForProject(projectId, Integer.MAX_VALUE);
    }

    /**
     * Proje için TTS ses üretir.
     * @param maxSceneNo Sadece bu sahne numarasına kadar olan sahneleri üretir (preview modu için)
     */
    public TurkuTtsService.TurkuTtsResponse generateForProject(UUID projectId, int maxSceneNo) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı: " + projectId));

        TurkuStoryboardPlan storyboard = project.getStoryboardPlan();
        if (storyboard == null || storyboard.getScenes() == null || storyboard.getScenes().isEmpty()) {
            throw new RuntimeException("Storyboard hazır değil — önce storyboard oluşturun");
        }

        List<TurkuTtsService.TurkuTtsResponse.AudioFile> audioFiles = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (TurkuStoryboardScene scene : storyboard.getScenes()) {
            // Preview modu: sadece maxSceneNo'ya kadar olan sahneleri üret
            if (scene.getSceneNo() > maxSceneNo) continue;

            String narration = scene.getNarrationSummary();
            if (narration == null || narration.isBlank()) {
                log.warn("Sahne {} için narration boş, atlanıyor", scene.getSceneNo());
                continue;
            }

            String path = String.format("turku-video/%s/audio/%d.mp3", projectId, scene.getSceneNo());

            try {
                log.info("ElevenLabs TTS üretiliyor: projectId={}, sahne={}, textLength={}, voice={}",
                        projectId, scene.getSceneNo(), narration.length(), voiceId);

                byte[] audioData = callElevenLabsApi(narration);
                uploadToMinio(path, audioData, "audio/mpeg");

                audioFiles.add(new TurkuTtsService.TurkuTtsResponse.AudioFile(
                        scene.getSceneNo(), path, audioData.length, true, null));
                successCount++;

                log.info("ElevenLabs TTS tamamlandı: sahne={}, size={} bytes",
                        scene.getSceneNo(), audioData.length);

            } catch (Exception e) {
                log.error("ElevenLabs TTS hatası: sahne={}, error={}", scene.getSceneNo(), e.getMessage());
                audioFiles.add(new TurkuTtsService.TurkuTtsResponse.AudioFile(
                        scene.getSceneNo(), null, 0, false, e.getMessage()));
                failureCount++;
            }
        }

        return TurkuTtsService.TurkuTtsResponse.builder()
                .projectId(projectId)
                .scenesProcessed(audioFiles.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .audioFiles(audioFiles)
                .build();
    }

    /**
     * ElevenLabs TTS API çağrısı.
     * POST /v1/text-to-speech/{voice_id}
     * Response: binary audio/mpeg
     */
    private byte[] callElevenLabsApi(String text) {
        // ElevenLabs max 5000 karakter, uzun metinleri parçala
        if (text.length() > 4500) {
            log.warn("Metin çok uzun ({}), kırpılıyor", text.length());
            text = text.substring(0, 4500);
        }

        Map<String, Object> body = Map.of(
                "text", text,
                "model_id", modelId,
                "voice_settings", Map.of(
                        "stability", stability,
                        "similarity_boost", similarityBoost,
                        "style", style,
                        "use_speaker_boost", true
                )
        );

        byte[] audioBytes = restClient.post()
                .uri("/text-to-speech/" + voiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept", "audio/mpeg")
                .body(body)
                .retrieve()
                .body(byte[].class);

        if (audioBytes == null || audioBytes.length == 0) {
            throw new RuntimeException("ElevenLabs API boş yanıt döndü");
        }

        return audioBytes;
    }

    private void uploadToMinio(String path, byte[] data, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(path)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("MinIO'ya yüklendi: path={}", path);
        } catch (Exception e) {
            throw new RuntimeException("MinIO yükleme hatası: " + path, e);
        }
    }
}
