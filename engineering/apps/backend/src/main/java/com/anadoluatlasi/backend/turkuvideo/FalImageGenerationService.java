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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fal.ai Flux Pro görsel üretim servisi.
 * Flux Pro v1.1 — son derece yüksek kaliteli, fotorealistik ve sanatsal görseller.
 *
 * <p>API: POST https://fal.run/fal-ai/flux-pro/v1.1
 * Fiyat: ~$0.05/görsel (Flux Pro)</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.image-provider", havingValue = "fal")
public class FalImageGenerationService {

    private final RestClient falClient;
    private final RestClient downloadClient;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final String model;
    private final String imageSize;
    private final int numInferenceSteps;
    private final double guidanceScale;

    public FalImageGenerationService(
            AiProviderProperties properties,
            MinioClient minioClient,
            MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;

        AiProviderProperties.Fal config = properties.getFal();
        this.model = config.getModel();
        this.imageSize = config.getImageSize();
        this.numInferenceSteps = config.getNumInferenceSteps();
        this.guidanceScale = config.getGuidanceScale();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(180));

        this.falClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Key " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(factory)
                .build();

        // Görsel indirmek için ayrı client (timeout uzun)
        SimpleClientHttpRequestFactory dlFactory = new SimpleClientHttpRequestFactory();
        dlFactory.setConnectTimeout(Duration.ofSeconds(15));
        dlFactory.setReadTimeout(Duration.ofSeconds(60));
        this.downloadClient = RestClient.builder()
                .requestFactory(dlFactory)
                .build();

        log.info("Fal.ai görsel servisi başlatıldı — model={}, size={}", model, imageSize);
    }

    /**
     * Sahne için Flux Pro görsel üretir ve MinIO'ya kaydeder.
     */
    public TurkuImageGenerationResult generateImage(
            TurkuImageGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope) {

        log.info("Fal.ai Flux Pro görsel üretimi başlatıldı: projectId={}, sceneNo={}",
                request.getProjectId(), request.getSceneNo());

        try {
            // 1. Prompt hazırla
            String prompt = buildFluxPrompt(request);

            // 2. Fal.ai API çağrısı
            byte[] imageBytes = callFalApi(prompt, request.getSceneNo());

            // 3. MinIO'ya kaydet
            String minioPath = uploadToMinio(request.getProjectId(), request.getSceneNo(), imageBytes);

            log.info("Fal.ai görsel üretimi tamamlandı: projectId={}, sceneNo={}, size={}KB",
                    request.getProjectId(), request.getSceneNo(), imageBytes.length / 1024);

            return TurkuImageGenerationResult.builder()
                    .projectId(request.getProjectId())
                    .sceneNo(request.getSceneNo())
                    .safeInternalRef(minioPath)
                    .success(true)
                    .traceRef(TurkuProviderTraceReference.builder()
                            .traceId(UUID.randomUUID().toString().substring(0, 8))
                            .generationStage("IMAGE_GENERATION")
                            .providerAlias("fal-flux-pro")
                            .timestamp(java.time.Instant.now().toString())
                            .build())
                    .needsHumanReview(true)
                    .build();

        } catch (Exception e) {
            log.error("Fal.ai görsel üretim hatası: projectId={}, sceneNo={}, error={}",
                    request.getProjectId(), request.getSceneNo(), e.getMessage());

            return TurkuImageGenerationResult.builder()
                    .projectId(request.getProjectId())
                    .sceneNo(request.getSceneNo())
                    .safeInternalRef("error-ref:" + UUID.randomUUID())
                    .success(false)
                    .safeErrorCode("FAL_IMAGE_GENERATION_FAILED")
                    .traceRef(TurkuProviderTraceReference.builder()
                            .traceId(UUID.randomUUID().toString().substring(0, 8))
                            .generationStage("IMAGE_GENERATION")
                            .providerAlias("fal-error")
                            .timestamp(java.time.Instant.now().toString())
                            .build())
                    .needsHumanReview(true)
                    .build();
        }
    }

    /**
     * Fal.ai Flux Pro API çağrısı.
     * POST /fal-ai/flux-pro/v1.1
     * Response: { "images": [{ "url": "...", "content_type": "image/jpeg" }] }
     */
    private byte[] callFalApi(String prompt, int sceneNo) {
        Map<String, Object> body = Map.of(
                "prompt", prompt,
                "image_size", imageSize,
                "num_inference_steps", numInferenceSteps,
                "guidance_scale", guidanceScale,
                "num_images", 1,
                "enable_safety_checker", false,
                "output_format", "png"
        );

        log.info("Fal.ai çağrılıyor: model={}, sceneNo={}, promptLen={}", model, sceneNo, prompt.length());

        FalResponse response = falClient.post()
                .uri("/" + model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(FalResponse.class);

        if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
            throw new RuntimeException("Fal.ai boş yanıt döndü");
        }

        String imageUrl = response.getImages().get(0).getUrl();
        log.info("Fal.ai görsel URL alındı: sceneNo={}, url={}", sceneNo, imageUrl.substring(0, Math.min(80, imageUrl.length())));

        // URL'den görseli indir
        return downloadImage(imageUrl);
    }

    private byte[] downloadImage(String url) {
        byte[] bytes = downloadClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("Fal.ai görsel indirilemedi");
        }

        log.debug("Görsel indirildi: size={}KB", bytes.length / 1024);
        return bytes;
    }

    /**
     * Sahne için sinematik, hikâye-tutarlı Flux Pro prompt'u hazırlar.
     *
     * <p>TEMEL İLKELER:
     * 1. GÖRSEL AÇIKLAMA HER ŞEYİN ÖNÜNDEDİR — Fal.ai promptun başını en çok dikkate alır
     * 2. Türkçe metin GÖNDERME — Fal.ai İngilizce model, Türkçeyi anlamaz
     * 3. Her sahne bir AKSİYON göstermeli — portre DEĞİL
     * 4. TALİMAT METNİ GÖNDERME — "FORBIDDEN", "DO NOT", "CRITICAL" gibi
     *    metinler modelin kafasını karıştırır ve metin posteri üretmesine neden olur.
     *    Sadece saf görsel açıklama gönder.</p>
     */
    private String buildFluxPrompt(TurkuImageGenerationRequest request) {
        StringBuilder sb = new StringBuilder();

        // ═══ 1. KARAKTERLERİN FİZİKSEL TANIMI (sahne aksiyonuyla doğal birleştirilmiş) ═══
        // Flux Pro talimat anlamaz — sadece "şunu görüyorum" formatında saf görsel açıklama.
        // Karakter tanımını sahne aksiyonuyla tek bir doğal cümle olarak birleştir.

        String visual = request.getSafePromptSummary();
        String charDesc = request.getCharacterDescription();

        if (charDesc != null && !charDesc.isBlank()) {
            // Karakter tanımı var — sahne açıklamasıyla birleştir
            if (visual != null && !visual.isBlank() && !visual.equals("Genel Anadolu manzarası")) {
                // Sahne açıklamasındaki "guitar" kelimesini temizle
                String cleanVisual = visual
                        .replaceAll("(?i)\\bguitar\\b", "bağlama")
                        .replaceAll("(?i)\\bacoustic guitar\\b", "bağlama")
                        .replaceAll("(?i)\\belectric guitar\\b", "bağlama");
                // Karakter tanımı + sahne aksiyonu birlikte
                sb.append(charDesc).append(", ").append(cleanVisual).append(". ");
            } else {
                // Sadece karakter tanımı
                sb.append(charDesc).append(". ");
            }
        } else if (visual != null && !visual.isBlank()) {
            // Karakter tanımı yok — sadece sahne açıklaması
            String cleanVisual = visual
                    .replaceAll("(?i)\\bguitar\\b", "bağlama")
                    .replaceAll("(?i)\\bacoustic guitar\\b", "bağlama");
            sb.append(cleanVisual).append(". ");
        }

        // ═══ 2. SİNEMATİK STİL VE DÖNEM ═══
        sb.append("Photorealistic cinematic film still, 35mm film grain, shallow depth of field. ");
        sb.append("19th century Ottoman Anatolian village setting, warm earth tones, natural lighting. ");

        // ═══ 3. KAMERA AÇISI (storyboard'dan geliyorsa onu kullan, yoksa değişken) ═══
        String cameraDir = request.getCameraDirection();
        if (cameraDir != null && !cameraDir.isBlank()) {
            sb.append(cameraDir).append(". ");
        } else {
            int sceneNo = request.getSceneNo();
            String cameraAngle = switch (sceneNo % 7) {
                case 0 -> "Extreme wide establishing shot";
                case 1 -> "Medium close-up portrait";
                case 2 -> "Over-the-shoulder shot";
                case 3 -> "Low angle dramatic shot";
                case 4 -> "Bird's eye view";
                case 5 -> "Dynamic diagonal composition";
                default -> "Wide environmental shot";
            };
            sb.append(cameraAngle).append(". ");
        }

        // ═══ 4. MOOD (kısa, doğal) ═══
        if (request.getSceneMood() != null && !request.getSceneMood().isBlank()) {
            sb.append(request.getSceneMood()).append(" atmosphere. ");
        }

        String prompt = sb.toString().trim();

        log.info("Fal.ai prompt sahne {}: {} chars", request.getSceneNo(), prompt.length());
        log.info("Fal.ai prompt sahne {} İÇERİK: {}", request.getSceneNo(), prompt);
        return prompt;
    }

    private String uploadToMinio(UUID projectId, int sceneNo, byte[] imageBytes) {
        String objectPath = String.format("turku-video/%s/scenes/%d.png", projectId, sceneNo);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                    .contentType("image/png")
                    .build());

            log.info("MinIO'ya yüklendi: path={}", objectPath);
            return objectPath;

        } catch (Exception e) {
            throw new RuntimeException("MinIO yükleme hatası: " + e.getMessage(), e);
        }
    }

    // ── Response DTOs ──

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FalResponse {
        private List<FalImage> images;
        private Map<String, Object> timings;
        private String seed;
        @JsonProperty("has_nsfw_concepts")
        private List<Boolean> hasNsfwConcepts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FalImage {
        private String url;
        @JsonProperty("content_type")
        private String contentType;
        private int width;
        private int height;
    }
}
