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

                // TTS telaffuz düzeltmesi — sayıları ve kısaltmaları düz metne çevir
                String processedNarration = preprocessForTts(narration);

                byte[] audioData = callElevenLabsApi(processedNarration);
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

    /**
     * TTS'e gönderilecek metni ön-işlemden geçir.
     * GPT bazen kurallara rağmen "19. yüzyıl" gibi sayılar yazabilir.
     * Bu metod güvenlik ağı olarak yaygın sayı/kısaltma kalıplarını
     * Türkçe düz metne çevirir.
     */
    private String preprocessForTts(String text) {
        if (text == null) return "";
        String result = text;

        // Yüzyıl kalıpları: "19. yüzyıl" → "on dokuzuncu yüzyıl"
        result = result.replace("19. yüzyıl", "on dokuzuncu yüzyıl");
        result = result.replace("19.yüzyıl", "on dokuzuncu yüzyıl");
        result = result.replace("20. yüzyıl", "yirminci yüzyıl");
        result = result.replace("18. yüzyıl", "on sekizinci yüzyıl");
        result = result.replace("17. yüzyıl", "on yedinci yüzyıl");
        result = result.replace("16. yüzyıl", "on altıncı yüzyıl");
        result = result.replace("15. yüzyıl", "on beşinci yüzyıl");

        // Yaygın yıllar
        result = result.replaceAll("\\b1860\\b", "bin sekiz yüz altmış");
        result = result.replaceAll("\\b1861\\b", "bin sekiz yüz altmış bir");
        result = result.replaceAll("\\b1862\\b", "bin sekiz yüz altmış iki");
        result = result.replaceAll("\\b1863\\b", "bin sekiz yüz altmış üç");
        result = result.replaceAll("\\b1864\\b", "bin sekiz yüz altmış dört");
        result = result.replaceAll("\\b1865\\b", "bin sekiz yüz altmış beş");
        result = result.replaceAll("\\b1866\\b", "bin sekiz yüz altmış altı");
        result = result.replaceAll("\\b1867\\b", "bin sekiz yüz altmış yedi");
        result = result.replaceAll("\\b1868\\b", "bin sekiz yüz altmış sekiz");
        result = result.replaceAll("\\b1869\\b", "bin sekiz yüz altmış dokuz");
        result = result.replaceAll("\\b1870\\b", "bin sekiz yüz yetmiş");
        result = result.replaceAll("\\b1880\\b", "bin sekiz yüz seksen");
        result = result.replaceAll("\\b1890\\b", "bin sekiz yüz doksan");
        result = result.replaceAll("\\b1900\\b", "bin dokuz yüz");
        result = result.replaceAll("\\b1915\\b", "bin dokuz yüz on beş");
        result = result.replaceAll("\\b1920\\b", "bin dokuz yüz yirmi");
        result = result.replaceAll("\\b1923\\b", "bin dokuz yüz yirmi üç");

        // Sıra sayıları: "1.", "2.", ... "35." — seslendirmede "birinci, ikinci..." olmalı
        // Ancak cümle içinde "Sahne 1." gibi kalıplar olabilir, dikkatli ol
        // Sadece küçük sayıları çevir (gün, yaş vb.)
        result = result.replaceAll("(?<=\\s)1\\. ", "birinci ");
        result = result.replaceAll("(?<=\\s)2\\. ", "ikinci ");
        result = result.replaceAll("(?<=\\s)3\\. ", "üçüncü ");
        result = result.replaceAll("(?<=\\s)4\\. ", "dördüncü ");
        result = result.replaceAll("(?<=\\s)5\\. ", "beşinci ");

        log.debug("TTS metin ön-işleme: orijinal={} chars, işlenmiş={} chars", text.length(), result.length());
        return result;
    }
}
