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
import java.util.ArrayList;
import java.util.List;
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
 *
 * <p>PARALEL MOD: submitToQueue ile tüm sahneleri aynı anda gönder,
 * sonra pollAndDownloadAll ile hepsini birden bekle. Bu 35 sahneyi
 * ~3 saatten ~5-7 dakikaya indirir.</p>
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

    // ═══════════════════════════════════════════════════════════════
    // PARALEL API: Submit → Poll All → Download All
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sahneyi Kling queue'ya gönderir (HEMEN döner, beklemez).
     * Sonuç: requestId ve URL'ler — polling için kullanılır.
     */
    public QueuedJob submitToQueue(UUID projectId, int sceneNo,
                                    String imageUrl, String motionPrompt) {
        try {
            String fullPrompt = motionPrompt + ". " + motionSuffix;

            KlingRequest request = KlingRequest.builder()
                    .imageUrl(imageUrl)
                    .prompt(fullPrompt)
                    .duration(duration)
                    .build();

            log.info("Kling submit: sahne={}, promptLen={}", sceneNo, fullPrompt.length());

            KlingQueueResponse queueResponse = falClient.post()
                    .uri("/" + model)
                    .body(request)
                    .retrieve()
                    .body(KlingQueueResponse.class);

            if (queueResponse == null || queueResponse.getRequestId() == null) {
                log.error("Kling submit başarısız: sahne={}, boş yanıt", sceneNo);
                return QueuedJob.failed(projectId, sceneNo, "Kling queue yanıtı boş");
            }

            log.info("Kling submit ✅ sahne={}, requestId={}", sceneNo, queueResponse.getRequestId());

            return QueuedJob.builder()
                    .projectId(projectId)
                    .sceneNo(sceneNo)
                    .requestId(queueResponse.getRequestId())
                    .statusUrl(queueResponse.getStatusUrl())
                    .responseUrl(queueResponse.getResponseUrl())
                    .submitted(true)
                    .build();

        } catch (Exception e) {
            log.error("Kling submit hatası: sahne={}, error={}", sceneNo, e.getMessage());
            return QueuedJob.failed(projectId, sceneNo, e.getMessage());
        }
    }

    /**
     * Tüm gönderilmiş işleri paralel olarak bekler, indirir ve MinIO'ya yükler.
     * Fal.ai bunları paralel işler → toplam süre tek sahne süresi kadar (~5dk).
     */
    public List<VideoGenerationResult> pollAndDownloadAll(List<QueuedJob> jobs) {
        List<VideoGenerationResult> results = new ArrayList<>();

        // Sadece başarılı submit'leri poll et
        List<QueuedJob> activeJobs = jobs.stream()
                .filter(QueuedJob::isSubmitted)
                .toList();

        // Başarısız submit'leri direkt sonuca ekle
        jobs.stream()
                .filter(j -> !j.isSubmitted())
                .forEach(j -> results.add(VideoGenerationResult.failure(j.getError())));

        if (activeJobs.isEmpty()) {
            return results;
        }

        log.info("Kling paralel polling başlıyor: {} iş", activeJobs.size());

        int maxAttempts = 90;  // 90 × 5sn = 7.5 dakika max (paralel olduğu için yeterli)

        // Her iş için tamamlanma durumu
        boolean[] completed = new boolean[activeJobs.size()];
        String[] videoUrls = new String[activeJobs.size()];
        int completedCount = 0;

        for (int attempt = 0; attempt < maxAttempts && completedCount < activeJobs.size(); attempt++) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            for (int i = 0; i < activeJobs.size(); i++) {
                if (completed[i]) continue;

                QueuedJob job = activeJobs.get(i);
                try {
                    String statusPath = job.getStatusUrl().replace("https://queue.fal.run", "");
                    KlingStatusResponse status = falClient.get()
                            .uri(statusPath)
                            .retrieve()
                            .body(KlingStatusResponse.class);

                    if (status == null) continue;

                    String statusStr = status.getStatus();

                    if ("COMPLETED".equalsIgnoreCase(statusStr)) {
                        // Video URL'sini al
                        String responsePath = job.getResponseUrl() != null
                                ? job.getResponseUrl().replace("https://queue.fal.run", "")
                                : statusPath.replace("/status", "");

                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = falClient.get()
                                .uri(responsePath)
                                .retrieve()
                                .body(Map.class);

                        String url = extractVideoUrl(result);
                        if (url != null) {
                            videoUrls[i] = url;
                        }
                        completed[i] = true;
                        completedCount++;
                        log.info("Kling ✅ sahne={} tamamlandı ({}/{})",
                                job.getSceneNo(), completedCount, activeJobs.size());

                    } else if ("FAILED".equalsIgnoreCase(statusStr)) {
                        completed[i] = true;
                        completedCount++;
                        log.error("Kling ❌ sahne={} başarısız", job.getSceneNo());
                    }
                } catch (Exception e) {
                    // Polling hatası — devam et, bir sonraki denemede tekrar dene
                    log.debug("Kling poll hatası sahne={}: {}", job.getSceneNo(), e.getMessage());
                }
            }

            // İlerleme logu (her 6 denemede = 30sn'de bir)
            if (attempt % 6 == 5) {
                log.info("Kling paralel polling: {}/{} tamamlandı, deneme={}/{}",
                        completedCount, activeJobs.size(), attempt + 1, maxAttempts);
            }
        }

        // Sonuçları işle: başarılı olanları indir ve MinIO'ya yükle
        for (int i = 0; i < activeJobs.size(); i++) {
            QueuedJob job = activeJobs.get(i);
            if (videoUrls[i] != null) {
                try {
                    byte[] videoBytes = downloadVideo(videoUrls[i]);
                    String minioPath = String.format("turku-video/%s/scenes/%d_video.mp4",
                            job.getProjectId(), job.getSceneNo());

                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(minioPath)
                            .stream(new ByteArrayInputStream(videoBytes), videoBytes.length, -1)
                            .contentType("video/mp4")
                            .build());

                    log.info("Kling video MinIO'ya yüklendi: sahne={}, size={}KB",
                            job.getSceneNo(), videoBytes.length / 1024);

                    results.add(VideoGenerationResult.success(minioPath, videoBytes.length));
                } catch (Exception e) {
                    log.error("Kling indirme/yükleme hatası: sahne={}, error={}",
                            job.getSceneNo(), e.getMessage());
                    results.add(VideoGenerationResult.failure(e.getMessage()));
                }
            } else {
                results.add(VideoGenerationResult.failure(
                        completed[i] ? "Video URL bulunamadı" : "Zaman aşımı"));
            }
        }

        log.info("Kling paralel tamamlandı: {} başarılı, {} başarısız",
                results.stream().filter(VideoGenerationResult::isSuccess).count(),
                results.stream().filter(r -> !r.isSuccess()).count());

        return results;
    }

    // ═══════════════════════════════════════════════════════════════
    // ESKİ TEKLİ API (geriye uyumluluk)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Statik görseli hareketli video klibe dönüştürür (SIRALİ — yavaş).
     * Yeni kod için submitToQueue + pollAndDownloadAll kullanın.
     */
    public VideoGenerationResult generateVideo(UUID projectId, int sceneNo,
                                                String imageUrl, String motionPrompt) {
        QueuedJob job = submitToQueue(projectId, sceneNo, imageUrl, motionPrompt);
        if (!job.isSubmitted()) {
            return VideoGenerationResult.failure(job.getError());
        }
        List<VideoGenerationResult> results = pollAndDownloadAll(List.of(job));
        return results.isEmpty() ? VideoGenerationResult.failure("Sonuç alınamadı") : results.get(0);
    }

    // ═══════════════════════════════════════════════════════════════
    // Yardımcı metodlar
    // ═══════════════════════════════════════════════════════════════

    private String extractVideoUrl(Map<String, Object> result) {
        if (result == null) return null;
        if (result.get("video") instanceof Map videoMap) {
            return (String) videoMap.get("url");
        }
        if (result.get("data") instanceof Map dataMap) {
            if (dataMap.get("video") instanceof Map videoMap2) {
                return (String) videoMap2.get("url");
            }
        }
        log.warn("Kling COMPLETED ama video URL bulunamadı: {}", result);
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

    // ═══════════════════════════════════════════════════════════════
    // DTO'lar
    // ═══════════════════════════════════════════════════════════════

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueuedJob {
        private UUID projectId;
        private int sceneNo;
        private String requestId;
        private String statusUrl;
        private String responseUrl;
        private boolean submitted;
        private String error;

        static QueuedJob failed(UUID projectId, int sceneNo, String error) {
            return QueuedJob.builder()
                    .projectId(projectId).sceneNo(sceneNo)
                    .submitted(false).error(error)
                    .build();
        }
    }

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

        /** Fal.ai'nin döndürdüğü polling URL — model path kısaltılmış olabilir */
        @JsonProperty("status_url")
        private String statusUrl;

        /** Fal.ai'nin döndürdüğü sonuç URL */
        @JsonProperty("response_url")
        private String responseUrl;
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
