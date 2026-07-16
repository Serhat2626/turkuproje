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
     * 4. Karakter tanımı sadece gerektiğinde ve KISA</p>
     */
    private String buildFluxPrompt(TurkuImageGenerationRequest request) {
        StringBuilder sb = new StringBuilder();

        // ═══ 1. ANA GÖRSEL AÇIKLAMA (EN ÖNEMLİ — prompt'un başı) ═══
        String visual = request.getSafePromptSummary();
        if (visual != null && !visual.isBlank() && !visual.equals("Genel Anadolu manzarası")) {
            sb.append(visual).append(". ");
        }

        // ═══ 2. SİNEMATİK STİL ═══
        sb.append("Photorealistic cinematic film still, 35mm film grain, shallow depth of field. ");
        sb.append("Natural golden hour lighting, warm earth tones. ");

        // ═══ 3. KAMERA AÇISI (sahne numarasına göre değişken) ═══
        int sceneNo = request.getSceneNo();
        String cameraAngle = switch (sceneNo % 7) {
            case 0 -> "Extreme wide establishing shot, vast Anatolian landscape";
            case 1 -> "Medium close-up, face partially visible, emotional expression";
            case 2 -> "Over-the-shoulder shot, looking at something in the distance";
            case 3 -> "Low angle dramatic shot, silhouette against sky";
            case 4 -> "Bird's eye view, figure small in vast landscape";
            case 5 -> "Dutch angle, dynamic tension, movement blur";
            default -> "Wide shot, full body in environment, environmental storytelling";
        };
        sb.append(cameraAngle).append(". ");

        // ═══ 4. KARAKTER (sadece görsel açıklamada karakter geçiyorsa) ═══
        if (request.getCharacterDescription() != null && !request.getCharacterDescription().isBlank()) {
            // Karakter tanımını kısalt — sadece fiziksel özellikler (max 300 char)
            String charDesc = request.getCharacterDescription();
            if (charDesc.length() > 300) charDesc = charDesc.substring(0, 300);
            sb.append(charDesc).append(". ");
        }

        // ═══ 5. MOOD/ATMOSFER ═══
        if (request.getSceneMood() != null && !request.getSceneMood().isBlank()) {
            sb.append("Emotional atmosphere: ").append(request.getSceneMood()).append(". ");
        }

        // ═══ 6. ÖNCEKİ SAHNE BAĞLANTISI (görsel süreklilik) ═══
        if (request.getPreviousSceneVisual() != null && !request.getPreviousSceneVisual().isBlank()) {
            String prev = request.getPreviousSceneVisual();
            if (prev.length() > 100) prev = prev.substring(0, 100);
            sb.append("Visual continuity from previous: ").append(prev).append(". ");
        }

        // ═══ 7. ANADOLU KÜLTÜREL DOĞRULUK ═══
        sb.append("CRITICAL CULTURAL ACCURACY: This is an Anatolian/Turkish folk story. ");
        sb.append("Any musical instrument MUST be a bağlama (Turkish long-necked lute/saz), NEVER a guitar. ");
        sb.append("The bağlama has a long thin neck with a small pear-shaped body. ");
        sb.append("Traditional Anatolian clothing: şalvar, yelek, headscarf for women. ");

        // ═══ 8. GÜÇLÜ YASAKLAR ═══
        sb.append("ABSOLUTELY FORBIDDEN: guitar, acoustic guitar, electric guitar, Western instruments, piano, violin, ");
        sb.append("looking directly at camera, static portrait pose, ");
        sb.append("two people standing side by side facing camera, symmetrical composition, ");
        sb.append("Arabic clothing, text, watermark, logo, modern elements, anime, cartoon. ");
        sb.append("Characters must be engaged in ACTION, never posing.");

        String prompt = sb.toString();
        log.debug("Fal.ai prompt sahne {}: {}", sceneNo,
                prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt);
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
