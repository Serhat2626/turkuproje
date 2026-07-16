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

        // ═══ 1. KARAKTER KİMLİK KARTI (EN BAŞTA — EN ÖNEMLİ) ═══
        // AI modeli prompt'un başındaki tanımı en güçlü şekilde uygular.
        // Karakter tanımı her sahnede aynı olmalı → tutarlılık.
        if (request.getCharacterDescription() != null && !request.getCharacterDescription().isBlank()) {
            String charDesc = request.getCharacterDescription();
            // Karakter kimlik kartı yeterince uzun olmalı — 800 char'a kadar izin ver
            if (charDesc.length() > 800) charDesc = charDesc.substring(0, 800);
            sb.append("EXACT CHARACTER IDENTITY — DO NOT DEVIATE FROM THIS DESCRIPTION: ");
            sb.append(charDesc).append(". ");
        }

        // ═══ 2. SAHNE AKSİYONU (karakter fiziksel tanımları TEMİZLENMİŞ) ═══
        // safePromptSummary'den karakter fiziksel tanımlarını çıkar,
        // sadece aksiyon/mekan/duygu bırak → karakter tanımı 1. bölümden gelsin.
        String visual = request.getSafePromptSummary();
        if (visual != null && !visual.isBlank() && !visual.equals("Genel Anadolu manzarası")) {
            String cleanedVisual = cleanCharacterDescriptionsFromVisual(visual);
            sb.append("SCENE ACTION: ").append(cleanedVisual).append(". ");
        }

        // ═══ 3. SİNEMATİK STİL ═══
        sb.append("Photorealistic cinematic film still, 35mm film grain, shallow depth of field. ");
        sb.append("Natural golden hour lighting, warm earth tones, 19th century Ottoman Anatolia. ");

        // ═══ 4. KAMERA AÇISI (sahne numarasına göre değişken) ═══
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

        // ═══ 5. MOOD/ATMOSFER ═══
        if (request.getSceneMood() != null && !request.getSceneMood().isBlank()) {
            sb.append("Emotional atmosphere: ").append(request.getSceneMood()).append(". ");
        }

        // ═══ 6. KÜLTÜREL DOĞRULUK (bağlama vs gitar) ═══
        sb.append("CULTURAL RULES: 19th century rural Anatolia. ");
        sb.append("Musical instrument = bağlama (Turkish saz with long thin neck, small pear-shaped wooden body). ");
        sb.append("Men wear: şalvar, yelek (vest), gömlek (shirt), çarık boots. ");
        sb.append("Women wear: entari dress, yaşmak headscarf, silver jewelry. ");

        // ═══ 7. GÜÇLÜ YASAKLAR ═══
        sb.append("ABSOLUTELY FORBIDDEN: guitar, acoustic guitar, any Western instrument, piano, violin, ");
        sb.append("modern framed photographs, glass bottles, modern furniture, ");
        sb.append("two men shown when scene describes a man and woman together, ");
        sb.append("changing character appearance between scenes, ");
        sb.append("Arabic/Middle-Eastern clothing, turban, ");
        sb.append("text, watermark, logo, modern elements, anime, cartoon, illustration.");

        String prompt = sb.toString();
        log.info("Fal.ai prompt sahne {}: karakter={} chars, toplam={} chars",
                sceneNo,
                request.getCharacterDescription() != null ? request.getCharacterDescription().length() : 0,
                prompt.length());
        log.debug("Fal.ai prompt sahne {} DETAY: {}", sceneNo, prompt);
        return prompt;
    }

    /**
     * safePromptSummary'den karakter fiziksel tanımlarını temizler.
     * GPT'nin ürettiği görsel açıklamada "young man with long dark hair" gibi
     * fiziksel tanımlar var — bunları çıkarıyoruz çünkü karakter tanımı
     * prompt'un en başında zaten sabit olarak duruyor.
     *
     * Temizlenen: yaş, saç, sakal, göz rengi, vücut tipi, kıyafet tanımları
     * Kalan: aksiyon, mekan, ışık, duygu, hava durumu, obje
     */
    private String cleanCharacterDescriptionsFromVisual(String visual) {
        if (visual == null) return "";

        String cleaned = visual;

        // Karakter fiziksel tanımlarını temizle (İngilizce — GPT İngilizce yazar)
        // "Young man with dark hair" → "man" (aksiyon kalır)
        cleaned = cleaned.replaceAll("(?i)\\byoung (handsome |beautiful |attractive )?", "");
        cleaned = cleaned.replaceAll("(?i)\\bwith (long |short |dark |curly |wavy |black |brown |blonde )+(hair|beard|stubble)\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(long|short|dark|curly|wavy|messy|flowing|braided) (dark |black |brown )?(hair|beard|locks|curls)\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(clean-shaven|bearded|stubbled|light stubble|thick beard)\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\bwearing [^,.]+(vest|shirt|dress|robe|cloak|coat|tunic|şalvar|entari)[^,.]*[,.]?", "");
        cleaned = cleaned.replaceAll("(?i)\\b(olive|wheat-colored|fair|dark|pale|tanned) skin\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(brown|green|blue|hazel|dark|deep) eyes\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(slender|athletic|muscular|petite|tall|strong) build\\b", "");
        cleaned = cleaned.replaceAll("(?i)\\b(handsome|beautiful|pretty|gorgeous) (man|woman|guy|girl|figure)\\b", "$2");

        // Gitar → bağlama zorla
        cleaned = cleaned.replaceAll("(?i)\\bguitar\\b", "bağlama (Turkish saz)");
        cleaned = cleaned.replaceAll("(?i)\\bacoustic guitar\\b", "bağlama (Turkish saz)");

        // Çift boşlukları temizle
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        // Baştaki virgül/nokta temizle
        cleaned = cleaned.replaceAll("^[,. ]+", "").trim();

        return cleaned;
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
