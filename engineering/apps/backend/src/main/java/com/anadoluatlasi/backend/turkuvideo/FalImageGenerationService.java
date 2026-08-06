package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.config.MinioConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fal.ai görsel üretim servisi — 2 aşamalı referans tabanlı üretim.
 *
 * <p>Aşama 1: Karakter referans görseli üret (flux-pro/v1.1, 1 kez)
 * Aşama 2: Her sahne için flux-pro/kontext kullan (referans ile tutarlı karakter)</p>
 *
 * <p>API'ler:
 * - Referans: POST https://fal.run/fal-ai/flux-pro/v1.1
 * - Sahne:    POST https://fal.run/fal-ai/flux-pro/kontext
 * - Upload:   PUT  https://fal.ai/api/storage/upload/v1 (Fal CDN)
 * Fiyat: ~$0.04/görsel</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.image-provider", havingValue = "fal")
public class FalImageGenerationService {

    private final RestClient falClient;
    private final RestClient downloadClient;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final String referenceModel;
    private final String kontextModel;
    private final String imageSize;
    private final int numInferenceSteps;
    private final double guidanceScale;
    private final String falApiKey;

    public FalImageGenerationService(
            AiProviderProperties properties,
            MinioClient minioClient,
            MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;

        AiProviderProperties.Fal config = properties.getFal();
        this.referenceModel = config.getModel();
        this.kontextModel = config.getKontextModel();
        this.imageSize = config.getImageSize();
        this.numInferenceSteps = config.getNumInferenceSteps();
        this.guidanceScale = config.getGuidanceScale();
        this.falApiKey = config.getApiKey();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(180));

        this.falClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Key " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(factory)
                .build();

        SimpleClientHttpRequestFactory dlFactory = new SimpleClientHttpRequestFactory();
        dlFactory.setConnectTimeout(Duration.ofSeconds(15));
        dlFactory.setReadTimeout(Duration.ofSeconds(60));
        this.downloadClient = RestClient.builder()
                .requestFactory(dlFactory)
                .build();

        log.info("Fal.ai görsel servisi başlatıldı — referenceModel={}, kontextModel={}, size={}",
                referenceModel, kontextModel, imageSize);
    }

    // ═══ AŞAMA 1: Karakter Referans Görseli ═══

    /**
     * Karakter referans görseli üretir (flux-pro/v1.1 ile, 1 kez).
     * MinIO'ya kaydeder + Fal.ai CDN'e upload eder.
     *
     * @return Fal.ai CDN URL (kontext'e geçmek için)
     */
    public String generateCharacterReference(UUID projectId, String charPrompt) {
        log.info("Karakter referans görseli üretiliyor: projectId={}", projectId);

        String minioPath = String.format("turku-video/%s/character_ref.png", projectId);

        // Zaten varsa sadece Fal CDN'e yükle
        if (existsInMinio(minioPath)) {
            log.info("Karakter referans görseli zaten mevcut, Fal CDN'e yükleniyor");
            byte[] existingBytes = downloadFromMinio(minioPath);
            String cdnUrl = uploadToFalCdn(existingBytes, projectId + "_ref.png");
            log.info("Mevcut referans Fal CDN'e yüklendi: {}", cdnUrl);
            return cdnUrl;
        }

        // Referans prompt: tam boy portre, nötr arka plan — illustration stili EN BAŞTA
        String refPrompt = "2D hand-drawn digital illustration, comic book panel art style, "
                + "cel-shaded coloring, bold clean outlines, flat color fills. "
                + "NOT a photograph, NOT photorealistic. "
                + charPrompt
                + ". Full body portrait, standing pose, facing camera, clear face details, "
                + "neutral light gray background, warm muted color palette.";

        byte[] imageBytes = callFalApi(refPrompt, referenceModel, null);
        uploadToMinioPath(minioPath, imageBytes);

        String cdnUrl = uploadToFalCdn(imageBytes, projectId + "_ref.png");
        log.info("Karakter referans görseli üretildi: size={}KB, cdnUrl={}",
                imageBytes.length / 1024, cdnUrl);

        return cdnUrl;
    }

    /**
     * Tüm karakterler için ayrı referans görselleri üretir.
     * Her karakter adıyla eşlenmiş CDN URL döner.
     * Anahtar: karakter adı (lowercase, trimmed), Değer: Fal CDN URL
     */
    public Map<String, String> generateAllCharacterReferences(UUID projectId, Map<String, String> characterPrompts) {
        Map<String, String> references = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : characterPrompts.entrySet()) {
            String charName = entry.getKey();
            String charPrompt = entry.getValue();
            String minioPath = String.format("turku-video/%s/character_ref_%s.png", projectId, charName.toLowerCase().replaceAll("\\s+", "_"));
            
            try {
                if (existsInMinio(minioPath)) {
                    log.info("Karakter referansı zaten mevcut: {}", charName);
                    byte[] existingBytes = downloadFromMinio(minioPath);
                    String cdnUrl = uploadToFalCdn(existingBytes, projectId + "_ref_" + charName.toLowerCase().replaceAll("\\s+", "_") + ".png");
                    references.put(charName, cdnUrl);
                    continue;
                }
                
                String refPrompt = "2D hand-drawn digital illustration, comic book panel art style, cel-shaded coloring, bold clean outlines, flat color fills. NOT a photograph, NOT photorealistic. " + charPrompt + ". Full body portrait, standing pose, facing slightly left, clear face details, neutral light gray background, warm muted color palette.";
                byte[] imageBytes = callFalApi(refPrompt, referenceModel, null);
                uploadToMinioPath(minioPath, imageBytes);
                String cdnUrl = uploadToFalCdn(imageBytes, projectId + "_ref_" + charName.toLowerCase().replaceAll("\\s+", "_") + ".png");
                references.put(charName, cdnUrl);
                log.info("✅ Karakter referansı üretildi: {} → {}", charName, cdnUrl.substring(0, Math.min(60, cdnUrl.length())));
            } catch (Exception e) {
                log.error("Karakter referans hatası: {} → {}", charName, e.getMessage());
            }
        }
        return references;
    }

    // ═══ AŞAMA 2: Sahne Görseli (Kontext) ═══

    /**
     * Sahne görseli üretir. Referans URL varsa Kontext, yoksa v1.1 fallback.
     */
    public TurkuImageGenerationResult generateImage(
            TurkuImageGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope) {

        log.info("Fal.ai görsel üretimi: projectId={}, sceneNo={}, hasRef={}",
                request.getProjectId(), request.getSceneNo(),
                request.getReferenceImageUrl() != null);

        try {
            String prompt;
            byte[] imageBytes;

            if (request.getReferenceImageUrl() != null && !request.getReferenceImageUrl().isBlank()) {
                prompt = buildKontextPrompt(request);
                imageBytes = callFalApi(prompt, kontextModel, request.getReferenceImageUrl());
            } else {
                prompt = buildFluxPrompt(request);
                imageBytes = callFalApi(prompt, referenceModel, null);
                log.warn("Referans yok, v1.1 fallback: sahne={}", request.getSceneNo());
            }

            String minioPath = uploadToMinio(request.getProjectId(), request.getSceneNo(), imageBytes);

            log.info("Görsel üretimi tamamlandı: sceneNo={}, size={}KB",
                    request.getSceneNo(), imageBytes.length / 1024);

            return TurkuImageGenerationResult.builder()
                    .projectId(request.getProjectId())
                    .sceneNo(request.getSceneNo())
                    .safeInternalRef(minioPath)
                    .success(true)
                    .traceRef(TurkuProviderTraceReference.builder()
                            .traceId(UUID.randomUUID().toString().substring(0, 8))
                            .generationStage("IMAGE_GENERATION")
                            .providerAlias(request.getReferenceImageUrl() != null ? "fal-kontext" : "fal-flux-pro")
                            .timestamp(java.time.Instant.now().toString())
                            .build())
                    .needsHumanReview(true)
                    .build();

        } catch (Exception e) {
            log.error("Görsel üretim hatası: sceneNo={}, error={}",
                    request.getSceneNo(), e.getMessage());

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

    // ═══ API Çağrıları ═══

    private byte[] callFalApi(String prompt, String model, String imageUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        body.put("image_size", imageSize);
        body.put("num_inference_steps", numInferenceSteps);
        body.put("guidance_scale", guidanceScale);
        body.put("num_images", 1);
        body.put("enable_safety_checker", false);
        body.put("output_format", "png");

        if (imageUrl != null && !imageUrl.isBlank()) {
            body.put("image_url", imageUrl);
        }

        log.info("Fal.ai çağrılıyor: model={}, promptLen={}, hasImageUrl={}",
                model, prompt.length(), imageUrl != null);

        FalResponse response = falClient.post()
                .uri("/" + model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(FalResponse.class);

        if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
            throw new RuntimeException("Fal.ai boş yanıt — model=" + model);
        }

        String resultUrl = response.getImages().get(0).getUrl();
        log.info("Fal.ai görsel URL alındı: {}",
                resultUrl.substring(0, Math.min(80, resultUrl.length())));

        return downloadImage(resultUrl);
    }

    /**
     * Görseli Fal.ai CDN'e yükler — Kontext bu URL'i referans olarak kullanır.
     * Fal.ai'nin localhost MinIO'ya erişimi yok, bu yüzden CDN upload gerekli.
     */
    private String uploadToFalCdn(byte[] imageBytes, String fileName) {
        try {
            SimpleClientHttpRequestFactory uploadFactory = new SimpleClientHttpRequestFactory();
            uploadFactory.setConnectTimeout(Duration.ofSeconds(15));
            uploadFactory.setReadTimeout(Duration.ofSeconds(60));

            RestClient uploadClient = RestClient.builder()
                    .requestFactory(uploadFactory)
                    .build();

            String uploadUrl = "https://fal.ai/api/storage/upload/v1/" + fileName;

            String response = uploadClient.put()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", "Key " + falApiKey)
                    .header("Content-Type", "image/png")
                    .body(imageBytes)
                    .retrieve()
                    .body(String.class);

            if (response != null && response.contains("https://")) {
                String cdnUrl = extractUrlFromResponse(response);
                log.info("Fal CDN'e yüklendi: {}", cdnUrl);
                return cdnUrl;
            }

            log.info("Fal CDN upload tamamlandı: {}", uploadUrl);
            return uploadUrl;

        } catch (Exception e) {
            log.warn("Fal CDN upload hatası: {} — base64 data URI kullanılıyor", e.getMessage());
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    private String extractUrlFromResponse(String response) {
        if (response.contains("\"url\"")) {
            int start = response.indexOf("\"url\"");
            int urlStart = response.indexOf("\"", start + 5) + 1;
            int urlEnd = response.indexOf("\"", urlStart);
            if (urlStart > 0 && urlEnd > urlStart) {
                return response.substring(urlStart, urlEnd);
            }
        }
        return response.trim();
    }

    private byte[] downloadImage(String url) {
        byte[] bytes = downloadClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("Görsel indirilemedi");
        }

        log.debug("Görsel indirildi: size={}KB", bytes.length / 1024);
        return bytes;
    }

    // ═══ Prompt Builders ═══

    /**
     * KONTEXT prompt — karakter tanımı YOK (referans görselden geliyor).
     * Sadece sahne aksiyonu + stil.
     */
    private String buildKontextPrompt(TurkuImageGenerationRequest request) {
        StringBuilder sb = new StringBuilder();

        // ⚠️ STİL EN BAŞTA — Flux Pro prompt başına ağırlık verir
        sb.append("2D hand-drawn digital illustration, comic book panel art style, ");
        sb.append("cel-shaded coloring, bold clean outlines, flat color fills, ");
        sb.append("NOT a photograph, NOT photorealistic, NOT 3D render. ");

        String visual = request.getSafePromptSummary();
        if (visual != null && !visual.isBlank() && !visual.equals("Genel Anadolu manzarası")) {
            String cleanVisual = sanitizeVisualPrompt(visual);
            sb.append("Transform the person in this image into this new scene. ");
            sb.append("Change the background, pose, and composition while keeping the person's face identical. ");
            sb.append(cleanVisual).append(". ");
        } else {
            sb.append("Place this person in a 19th century Anatolian village scene. ");
        }

        sb.append("Keep ONLY the face and facial features identical. ");
        sb.append("19th century Ottoman Anatolian village, warm muted earth tones, painterly backgrounds. ");

        String cameraDir = request.getCameraDirection();
        if (cameraDir != null && !cameraDir.isBlank()) {
            sb.append(cameraDir).append(". ");
        }

        if (request.getSceneMood() != null && !request.getSceneMood().isBlank()) {
            sb.append(request.getSceneMood()).append(" atmosphere. ");
        }

        return sb.toString().trim();
    }

    /**
     * v1.1 fallback prompt (referans yoksa).
     */
    private String buildFluxPrompt(TurkuImageGenerationRequest request) {
        StringBuilder sb = new StringBuilder();

        // ⚠️ STİL EN BAŞTA — Flux Pro prompt başına ağırlık verir
        sb.append("2D hand-drawn digital illustration, comic book panel art style, ");
        sb.append("cel-shaded coloring, bold clean outlines, flat color fills, ");
        sb.append("NOT a photograph, NOT photorealistic, NOT 3D render. ");

        String visual = request.getSafePromptSummary();
        String charDesc = request.getCharacterDescription();

        if (charDesc != null && !charDesc.isBlank()) {
            if (visual != null && !visual.isBlank() && !visual.equals("Genel Anadolu manzarası")) {
                String cleanVisual = sanitizeVisualPrompt(visual);
                sb.append(charDesc).append(", ").append(cleanVisual).append(". ");
            } else {
                sb.append(charDesc).append(". ");
            }
        } else if (visual != null && !visual.isBlank()) {
            sb.append(sanitizeVisualPrompt(visual)).append(". ");
        }

        sb.append("19th century Ottoman Anatolian village, warm muted earth tones, painterly backgrounds. ");

        String cameraDir = request.getCameraDirection();
        if (cameraDir != null && !cameraDir.isBlank()) {
            sb.append(cameraDir).append(". ");
        }

        if (request.getSceneMood() != null && !request.getSceneMood().isBlank()) {
            sb.append(request.getSceneMood()).append(" atmosphere. ");
        }

        return sb.toString().trim();
    }

    // ═══ Türkçe Tespit & Temizleme ═══

    /**
     * Görsel prompt'u temizler:
     * 1. Türkçe metin tespiti — uyarı loglar
     * 2. guitar → bağlama düzeltmesi
     * 3. Yaygın Türkçe kelimeleri İngilizceye çevirir
     */
    private String sanitizeVisualPrompt(String visual) {
        if (visual == null || visual.isBlank()) return visual;

        String cleaned = visual
                // Enstrüman düzeltmesi
                .replaceAll("(?i)\\bguitar\\b", "bağlama")
                .replaceAll("(?i)\\bacoustic guitar\\b", "bağlama")
                .replaceAll("(?i)\\blute\\b", "bağlama")
                .replaceAll("(?i)\\boud\\b", "bağlama");

        // Türkçe karakter tespiti (ç, ş, ğ, ü, ö, ı, İ)
        boolean hasTurkish = cleaned.matches(".*[çşğüöıİÇŞĞÜÖ].*")
                || cleaned.matches("(?i).*(köy|dağ|göl|ırmak|çeşme|mektup|ayrılık|buluşma|savaş|asker|yaşlı|genç|kadın|erkek|ağlama|veda|hasret|gurbet|yalnızlık|bahar|kış|sonbahar|yaz|gece|gündüz|sabah|akşam).*");

        if (hasTurkish) {
            log.warn("⚠️ Görsel prompt'ta Türkçe tespit edildi! Temizleniyor: {}",
                    cleaned.substring(0, Math.min(80, cleaned.length())));

            // Yaygın Türkçe kelimeleri İngilizceye çevir
            cleaned = cleaned
                    // Mekanlar
                    .replaceAll("(?i)\\bköy\\b", "village")
                    .replaceAll("(?i)\\bdağ\\b", "mountain")
                    .replaceAll("(?i)\\bgöl\\b", "lake")
                    .replaceAll("(?i)\\bırmak\\b", "river")
                    .replaceAll("(?i)\\bçeşme\\b", "fountain")
                    .replaceAll("(?i)\\bev\\b", "house")
                    .replaceAll("(?i)\\byol\\b", "road")
                    .replaceAll("(?i)\\btarla\\b", "field")
                    .replaceAll("(?i)\\bahır\\b", "stable")
                    .replaceAll("(?i)\\bçayır\\b", "meadow")
                    // İnsanlar
                    .replaceAll("(?i)\\bkadın\\b", "woman")
                    .replaceAll("(?i)\\berkek\\b", "man")
                    .replaceAll("(?i)\\bgenç\\b", "young")
                    .replaceAll("(?i)\\byaşlı\\b", "old")
                    .replaceAll("(?i)\\basker\\b", "soldier")
                    .replaceAll("(?i)\\bçocuk\\b", "child")
                    .replaceAll("(?i)\\bana\\b", "mother")
                    .replaceAll("(?i)\\bbaba\\b", "father")
                    // Eylemler
                    .replaceAll("(?i)\\bağlama\\b", "crying")
                    .replaceAll("(?i)\\bağlıyor\\b", "crying")
                    .replaceAll("(?i)\\byürüyor\\b", "walking")
                    .replaceAll("(?i)\\bkoşuyor\\b", "running")
                    .replaceAll("(?i)\\boturuyor\\b", "sitting")
                    .replaceAll("(?i)\\byazıyor\\b", "writing")
                    .replaceAll("(?i)\\bbakıyor\\b", "looking")
                    .replaceAll("(?i)\\bbekliyor\\b", "waiting")
                    .replaceAll("(?i)\\bveda\\b", "farewell")
                    .replaceAll("(?i)\\bbuluşma\\b", "meeting")
                    .replaceAll("(?i)\\bayrılık\\b", "separation")
                    .replaceAll("(?i)\\bmektup\\b", "letter")
                    // Doğa / Hava
                    .replaceAll("(?i)\\bbahar\\b", "spring")
                    .replaceAll("(?i)\\byaz\\b", "summer")
                    .replaceAll("(?i)\\bsonbahar\\b", "autumn")
                    .replaceAll("(?i)\\bkış\\b", "winter")
                    .replaceAll("(?i)\\bgece\\b", "night")
                    .replaceAll("(?i)\\bgündüz\\b", "daytime")
                    .replaceAll("(?i)\\bsabah\\b", "morning")
                    .replaceAll("(?i)\\bakşam\\b", "evening")
                    .replaceAll("(?i)\\bgüneş\\b", "sun")
                    .replaceAll("(?i)\\bay\\b", "moon")
                    .replaceAll("(?i)\\byağmur\\b", "rain")
                    .replaceAll("(?i)\\bkar\\b", "snow")
                    .replaceAll("(?i)\\bsis\\b", "fog")
                    .replaceAll("(?i)\\brüzgar\\b", "wind")
                    // Duygular
                    .replaceAll("(?i)\\bhüzün\\b", "sorrow")
                    .replaceAll("(?i)\\bhasret\\b", "longing")
                    .replaceAll("(?i)\\bgurbet\\b", "exile")
                    .replaceAll("(?i)\\byalnızlık\\b", "loneliness")
                    .replaceAll("(?i)\\bumut\\b", "hope")
                    .replaceAll("(?i)\\bsavaş\\b", "war")
                    .replaceAll("(?i)\\bölüm\\b", "death");
        }

        return cleaned;
    }

    // ═══ MinIO İşlemleri ═══

    private String uploadToMinio(UUID projectId, int sceneNo, byte[] imageBytes) {
        String objectPath = String.format("turku-video/%s/scenes/%d.png", projectId, sceneNo);
        uploadToMinioPath(objectPath, imageBytes);
        return objectPath;
    }

    private void uploadToMinioPath(String objectPath, byte[] imageBytes) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                    .contentType("image/png")
                    .build());
            log.info("MinIO'ya yüklendi: path={}", objectPath);
        } catch (Exception e) {
            throw new RuntimeException("MinIO yükleme hatası: " + e.getMessage(), e);
        }
    }

    private boolean existsInMinio(String objectPath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectPath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] downloadFromMinio(String objectPath) {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectPath)
                .build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("MinIO indirme hatası: " + e.getMessage(), e);
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
