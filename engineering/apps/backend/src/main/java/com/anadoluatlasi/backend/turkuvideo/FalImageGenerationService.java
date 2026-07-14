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
     * <p>Prompt stratejisi:
     * 1. SABİT STİL PREFİX — tüm görsellerde aynı sinematik dil
     * 2. HİKÂYE BAĞLAMI — AI, hikâyenin neresinde olduğunu bilir
     * 3. KARAKTER TANIMI — aynı karakter her sahnede tutarlı
     * 4. ÖNCEKİ SAHNE — görsel akış sağlar
     * 5. SAHNE DETAYI — spesifik sahne açıklaması
     * 6. MOOD/ATMOSFER — ışık ve renk yönlendirmesi</p>
     */
    private String buildFluxPrompt(TurkuImageGenerationRequest request) {
        StringBuilder sb = new StringBuilder();

        // 1. SABİT STİL — tüm sahnelerde tutarlı sinematik dil
        sb.append("Photorealistic cinematic film still, shot on 35mm film, Anatolian historical drama. ");
        sb.append("Natural lighting, rich earth tones, warm golden hour palette. ");
        sb.append("Ottoman/Anatolian village setting, period-accurate costumes and architecture. ");

        // 2. SAHNE POZİSYONU — hikâyenin neresinde?
        sb.append(String.format("Scene %d of %d in a visual story sequence. ",
                request.getSceneNo(), request.getTotalScenes() > 0 ? request.getTotalScenes() : 25));

        // 3. KARAKTER TANIMI — tutarlı karakter görünümü (EN ÖNEMLİ)
        if (request.getCharacterDescription() != null && !request.getCharacterDescription().isBlank()) {
            sb.append(request.getCharacterDescription()).append(". ");
        }

        // 4. HİKÂYENİN BU ANI — narration text'ten hangi olay/an olduğunu bildir
        if (request.getNarrationText() != null && !request.getNarrationText().isBlank()) {
            // Narration'dan kısa özet çıkar (prompt çok uzamasın)
            String narration = request.getNarrationText();
            if (narration.length() > 300) narration = narration.substring(0, 300);
            sb.append("THIS MOMENT IN THE STORY: ").append(narration).append(". ");
        }

        // 5. SAHNE BAŞLIĞI
        if (request.getSceneTitle() != null && !request.getSceneTitle().isBlank()) {
            sb.append("Scene title: ").append(request.getSceneTitle()).append(". ");
        }

        // 6. ÖNCEKİ SAHNE — görsel akış sürekliliği
        if (request.getPreviousSceneVisual() != null && !request.getPreviousSceneVisual().isBlank()) {
            String prevVisual = request.getPreviousSceneVisual();
            if (prevVisual.length() > 150) prevVisual = prevVisual.substring(0, 150);
            sb.append("Previous scene showed: ").append(prevVisual).append(". This scene continues the visual narrative. ");
        }

        // 7. BU SAHNENİN GÖRSEL DETAYI — storyboard'daki visual description
        sb.append("VISUAL COMPOSITION: ").append(request.getSafePromptSummary()).append(". ");

        // 8. MOOD/ATMOSFER
        if (request.getSceneMood() != null && !request.getSceneMood().isBlank()) {
            sb.append("Mood and atmosphere: ").append(request.getSceneMood()).append(". ");
        }

        // 9. HİKÂYE BAĞLAMI — genel hikâye
        if (request.getStoryContext() != null && !request.getStoryContext().isBlank()) {
            String ctx = request.getStoryContext();
            if (ctx.length() > 200) ctx = ctx.substring(0, 200);
            sb.append("Overall story: ").append(ctx).append(". ");
        }

        // 10. KALİTE ve YASAK notları
        sb.append("Ultra high detail, cinematic composition, emotional depth, ");
        sb.append("professional film photography, consistent characters throughout. ");
        sb.append("No text, no watermarks, no logos, no modern elements, no animation style.");

        return sb.toString();
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
