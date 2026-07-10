package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
 * OpenAI TTS (Text-to-Speech) servisi.
 * Storyboard sahnelerinin narration metinlerini ses dosyasına çevirir.
 *
 * <p>Voice: 'nova' — sıcak, doğal, belgesel anlatımı için ideal.
 * Model: 'tts-1-hd' — yüksek kaliteli ses (~$0.030/1K karakter).
 * Speed: 0.9 — hafif yavaş, belgesel tarzı.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class TurkuTtsService {

    private final RestClient restClient;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final TurkuVideoProjectRepository repository;

    public TurkuTtsService(
            AiProviderProperties properties,
            MinioClient minioClient,
            MinioConfig minioConfig,
            TurkuVideoProjectRepository repository) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.repository = repository;

        AiProviderProperties.OpenAi config = properties.getOpenai();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(120));

        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(factory)
                .build();
    }

    /**
     * Proje için tüm sahnelerin seslendirmesini üret.
     */
    public TurkuTtsResponse generateForProject(UUID projectId) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı: " + projectId));

        TurkuStoryboardPlan storyboard = project.getStoryboardPlan();
        if (storyboard == null || storyboard.getScenes() == null || storyboard.getScenes().isEmpty()) {
            throw new RuntimeException("Storyboard hazır değil — önce storyboard oluşturun");
        }

        List<TurkuTtsResponse.AudioFile> audioFiles = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (TurkuStoryboardScene scene : storyboard.getScenes()) {
            String narration = scene.getNarrationSummary();
            if (narration == null || narration.isBlank()) {
                log.warn("Sahne {} için narration boş, atlanıyor", scene.getSceneNo());
                continue;
            }

            String path = String.format("turku-video/%s/audio/%d.mp3", projectId, scene.getSceneNo());

            try {
                log.info("TTS üretiliyor: projectId={}, sahne={}, textLength={}",
                        projectId, scene.getSceneNo(), narration.length());

                byte[] audioData = callTtsApi(narration);
                uploadToMinio(path, audioData, "audio/mpeg");

                audioFiles.add(new TurkuTtsResponse.AudioFile(
                        scene.getSceneNo(), path, audioData.length, true, null));
                successCount++;

                log.info("TTS tamamlandı: sahne={}, size={} bytes", scene.getSceneNo(), audioData.length);

            } catch (Exception e) {
                log.error("TTS hatası: sahne={}, error={}", scene.getSceneNo(), e.getMessage());
                audioFiles.add(new TurkuTtsResponse.AudioFile(
                        scene.getSceneNo(), null, 0, false, e.getMessage()));
                failureCount++;
            }
        }

        return TurkuTtsResponse.builder()
                .projectId(projectId)
                .scenesProcessed(audioFiles.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .audioFiles(audioFiles)
                .build();
    }

    /**
     * OpenAI TTS API çağrısı — binary MP3 döndürür.
     */
    private byte[] callTtsApi(String text) {
        Map<String, Object> body = Map.of(
                "model", "tts-1-hd",
                "input", text,
                "voice", "nova",
                "speed", 0.9,
                "response_format", "mp3"
        );

        byte[] audioBytes = restClient.post()
                .uri("/audio/speech")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(byte[].class);

        if (audioBytes == null || audioBytes.length == 0) {
            throw new RuntimeException("TTS API boş yanıt döndü");
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

    // ── Response DTO ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurkuTtsResponse {
        private UUID projectId;
        private int scenesProcessed;
        private int successCount;
        private int failureCount;
        private List<AudioFile> audioFiles;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AudioFile {
            private int sceneNo;
            private String path;
            private long sizeBytes;
            private boolean success;
            private String error;
        }
    }
}
