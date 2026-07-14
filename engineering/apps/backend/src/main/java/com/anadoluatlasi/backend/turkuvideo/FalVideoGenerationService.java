package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.config.MinioConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Fal.ai Kling AI Image-to-Video servisi.
 * Statik görseli hareketli video klibe dönüştürür.
 *
 * <p>API: Fal.ai Queue API → Kling Video v2/v3
 * Akış: Görsel URL → Kling API → Video URL → İndir → MinIO'ya yükle</p>
 *
 * <p>Fal.ai Queue API kullanılır çünkü video üretimi uzun sürer (30-120sn):
 * 1. POST /queue/submit → requestId alınır
 * 2. GET /queue/status/{requestId} → tamamlanana kadar polling
 * 3. GET /queue/result/{requestId} → video URL alınır</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.video-provider", havingValue = "kling")
public class FalVideoGenerationService {

    private final RestClient falClient;
    private final RestClient downloadClient;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final String model;
    private final String duration;
    private final String motionSuffix;

    public FalVideoGenerationService(
            AiProviderProperties properties,
            MinioClient minioClient,
            MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;

        AiProviderProperties.Kling config = properties.getKling();
        this.model = config.getModel();
        this.duration = config.getDuration();
        this.motionSuffix = config.getMotionPromptSuffix();

        // Fal.ai API key — görsel servisiyle aynı key
        String apiKey = properties.getFal().getApiKey();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.falClient = RestClient.builder()
                .baseUrl("https://queue.fal.run")
                .defaultHeader("Authorization", "Key " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(factory)
                .build();

        // Video indirmek için client (büyük dosya, uzun timeout)
        SimpleClientHttpRequestFactory dlFactory = new SimpleClientHttpRequestFactory();
        dlFactory.setConnectTimeout(Duration.ofSeconds(30));
        dlFactory.setReadTimeout(Duration.ofSeconds(300));
        this.downloadClient = RestClient.builder()
                .requestFactory(dlFactory)
                .build();

        log.info("Kling AI Video servisi başlatıldı — model={}, duration={}s", model, duration);
    }

    /**
     * Statik görseli hareketli video klibe dönüştürür.
     *
     * @param projectId   Proje ID
     * @param sceneNo     Sahne numarası
     * @param imageUrl    Görsel URL (MinIO veya public URL)
     * @param motionPrompt Hareket prompt'u (ör: "Rüzgarda sallanan ağaçlar, yürüyen insanlar")
     * @return Video üretim sonucu
     */
    public VideoGenerationResult generateVideo(UUID projectId, int sceneNo,
                                                String imageUrl, String motionPrompt) {
        try {
            log.info("Kling video üretimi başlıyor — scene={}, prompt={}",
                    sceneNo, motionPrompt.substring(0, Math.min(80, motionPrompt.length())));

            // 1. Queue'ya gönder
            String fullPrompt = motionPrompt + ". " + motionSuffix;

            KlingRequest request = KlingRequest.builder()
                    .imageUrl(imageUrl)
                    .prompt(fullPrompt)
                    .duration(duration)
                    .build();

            KlingQueueResponse queueResponse = falClient.post()
                    .uri("/" + model)
                    .body(request)
                    .retrieve()
                    .body(KlingQueueResponse.class);

            if (queueResponse == null || queueResponse.getRequestId() == null) {
                return VideoGenerationResult.failure("Kling queue yanıtı boş");
            }

            String requestId = queueResponse.getRequestId();
            log.info("Kling queue'ya eklendi — requestId={}", requestId);

            // 2. Polling — tamamlanana kadar bekle (max 5 dakika)
            String videoUrl = pollForResult(requestId);
            if (videoUrl == null) {
                return VideoGenerationResult.failure("Kling video üretimi zaman aşımı");
            }

            // 3. Video'yu indir
            byte[] videoBytes = downloadVideo(videoUrl);
            log.info("Kling video indirildi — size={}KB", videoBytes.length / 1024);

            // 4. MinIO'ya yükle
            String minioPath = String.format("turku-video/%s/scenes/%d_video.mp4", projectId, sceneNo);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(minioPath)
                    .stream(new ByteArrayInputStream(videoBytes), videoBytes.length, -1)
                    .contentType("video/mp4")
                    .build());

            log.info("Kling video MinIO'ya yüklendi — path={}, size={}KB",
                    minioPath, videoBytes.length / 1024);

            return VideoGenerationResult.success(minioPath, videoBytes.length);

        } catch (Exception e) {
            log.error("Kling video üretim hatası — scene={}: {}", sceneNo, e.getMessage());
            return VideoGenerationResult.failure(e.getMessage());
        }
    }

    /**
     * Queue polling — video hazır olana kadar bekle.
     */
    private String pollForResult(String requestId) throws InterruptedException {
        int maxAttempts = 60;  // 60 * 5sn = 5 dakika max
        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(5000);  // 5 saniye bekle

            try {
                KlingStatusResponse status = falClient.get()
                        .uri("/" + model + "/requests/" + requestId + "/status")
                        .retrieve()
                        .body(KlingStatusResponse.class);

                if (status == null) continue;

                String statusStr = status.getStatus();
                log.debug("Kling polling — attempt={}, status={}", i + 1, statusStr);

                if ("COMPLETED".equalsIgnoreCase(statusStr)) {
                    // Sonucu al
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = falClient.get()
                            .uri("/" + model + "/requests/" + requestId)
                            .retrieve()
                            .body(Map.class);

                    if (result != null && result.get("video") instanceof Map videoMap) {
                        return (String) videoMap.get("url");
                    }
                    // Alternatif response yapısı
                    if (result != null && result.get("data") instanceof Map dataMap) {
                        if (dataMap.get("video") instanceof Map videoMap2) {
                            return (String) videoMap2.get("url");
                        }
                    }
                    log.warn("Kling COMPLETED ama video URL bulunamadı: {}", result);
                    return null;
                }

                if ("FAILED".equalsIgnoreCase(statusStr)) {
                    log.error("Kling video üretimi başarısız: {}", status);
                    return null;
                }
            } catch (Exception e) {
                log.debug("Kling polling hatası (devam ediyor): {}", e.getMessage());
            }
        }

        log.error("Kling video üretimi zaman aşımı — {} deneme yapıldı", maxAttempts);
        return null;
    }

    private byte[] downloadVideo(String url) {
        byte[] bytes = downloadClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("Kling video indirilemedi");
        }
        return bytes;
    }

    // ── DTO'lar ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class KlingRequest {
        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("prompt")
        private String prompt;

        @JsonProperty("duration")
        private String duration;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class KlingQueueResponse {
        @JsonProperty("request_id")
        private String requestId;

        @JsonProperty("status")
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class KlingStatusResponse {
        @JsonProperty("status")
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoGenerationResult {
        private boolean success;
        private String minioPath;
        private long sizeBytes;
        private String error;

        static VideoGenerationResult success(String path, long size) {
            return VideoGenerationResult.builder()
                    .success(true).minioPath(path).sizeBytes(size).build();
        }

        static VideoGenerationResult failure(String error) {
            return VideoGenerationResult.builder()
                    .success(false).error(error).build();
        }
    }
}
