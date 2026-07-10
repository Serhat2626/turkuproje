package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.config.MinioConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.minio.GetObjectArgs;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI destekli Türkü video üretim gateway'i.
 *
 * <p>Türkünün hikâyesi, senaryosu ve storyboard'u için özelleştirilmiş
 * Türkçe system prompt'ları kullanarak GPT-4o çağrısı yapar.</p>
 *
 * <p>DALL-E 3 ile sahne görselleri üretir, MinIO'ya kaydeder.</p>
 *
 * <p>API key, provider response, raw token sayıları loglanmaz.
 * Şarkı sözleri (lyrics) üretilmez veya saklanmaz.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiTurkuGenerationProviderGateway implements TurkuGenerationProviderGateway {

    private final RestClient restClient;
    private final AiProviderProperties.OpenAi config;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /** Ayrı bir RestClient — DALL-E image generation daha uzun sürer */
    private final RestClient imageRestClient;

    /** Görsel indirmek için timeout'suz basit bir RestClient */
    private final RestClient downloadRestClient;

    public OpenAiTurkuGenerationProviderGateway(AiProviderProperties properties,
                                                 MinioClient minioClient,
                                                 MinioConfig minioConfig) {
        this.config = properties.getOpenai();
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(factory)
                .build();

        // DALL-E 3 image generation — 120 saniye timeout (görsel üretim yavaş olabilir)
        SimpleClientHttpRequestFactory imageFactory = new SimpleClientHttpRequestFactory();
        imageFactory.setConnectTimeout(Duration.ofSeconds(30));
        imageFactory.setReadTimeout(Duration.ofSeconds(120));

        this.imageRestClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(imageFactory)
                .build();

        // Üretilen görsel URL'sini indirmek için ayrı client (auth gerektirmez)
        SimpleClientHttpRequestFactory downloadFactory = new SimpleClientHttpRequestFactory();
        downloadFactory.setConnectTimeout(Duration.ofSeconds(15));
        downloadFactory.setReadTimeout(Duration.ofSeconds(60));

        this.downloadRestClient = RestClient.builder()
                .requestFactory(downloadFactory)
                .build();
    }

    // ── AI Text Generation ──────────────────────────────────

    @Override
    public TurkuAiGenerationResult generateAi(
            TurkuAiGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope) {

        String stage = request.getGenerationStage();
        TurkuNarrativeContext ctx = request.getContext();

        log.info("OpenAI Türkü üretimi başlatıldı: projectId={}, stage={}",
                request.getProjectId(), stage);

        // Prompt hazırla
        String systemPrompt = buildSystemPrompt(stage);
        String userPrompt = buildUserPrompt(stage, ctx, request.getSafeParams());

        // OpenAI çağrısı
        String aiContent;
        try {
            aiContent = callOpenAi(systemPrompt, userPrompt, request.getProjectId());
        } catch (Exception e) {
            log.error("OpenAI Türkü üretim hatası: projectId={}, stage={}, error={}",
                    request.getProjectId(), stage, e.getMessage());

            return TurkuAiGenerationResult.builder()
                    .projectId(request.getProjectId())
                    .generationStage(stage)
                    .traceRef(buildTrace(stage, "openai-error"))
                    .safeOutputSummary("OpenAI üretim hatası: " + e.getMessage())
                    .outputConfidence(TurkuSourceConfidence.LOW)
                    .needsHumanReview(true)
                    .copyrightReviewRequired(true)
                    .reviewFlags(List.of("HUMAN_REVIEW_REQUIRED", "GENERATION_ERROR"))
                    .safeWarnings(List.of("OpenAI çağrısı başarısız oldu: " + e.getMessage()))
                    .build();
        }

        log.info("OpenAI Türkü üretimi tamamlandı: projectId={}, stage={}, contentLength={}",
                request.getProjectId(), stage, aiContent.length());

        return TurkuAiGenerationResult.builder()
                .projectId(request.getProjectId())
                .generationStage(stage)
                .traceRef(buildTrace(stage, "openai-gpt4o"))
                .safeOutputSummary(aiContent)
                .outputConfidence(TurkuSourceConfidence.MEDIUM)
                .needsHumanReview(true)
                .copyrightReviewRequired(true)
                .reviewFlags(List.of(
                        "HUMAN_REVIEW_REQUIRED",
                        "COPYRIGHT_REVIEW_REQUIRED",
                        "CULTURAL_CONTEXT_REVIEW_REQUIRED",
                        "LYRICS_NOT_INCLUDED",
                        "AI_GENERATED_CONTENT"))
                .safeWarnings(List.of("AI üretimi — insan incelemesi gerekli"))
                .build();
    }

    // ── Image Generation (DALL-E 3) ─────────────────────────

    @Override
    public TurkuImageGenerationResult generateImage(
            TurkuImageGenerationRequest request,
            TurkuGenerationSafetyEnvelope envelope) {

        log.info("gpt-image-1 görsel üretimi başlatıldı: projectId={}, sceneNo={}",
                request.getProjectId(), request.getSceneNo());

        try {
            // 1. gpt-image-1 API çağrısı — b64_json olarak döner
            byte[] imageBytes = callImageGeneration(request);

            // 2. MinIO'ya kaydet
            String minioPath = uploadToMinio(request.getProjectId(), request.getSceneNo(), imageBytes);

            log.info("gpt-image-1 görsel üretimi tamamlandı: projectId={}, sceneNo={}, minioPath={}, size={}KB",
                    request.getProjectId(), request.getSceneNo(), minioPath, imageBytes.length / 1024);

            return TurkuImageGenerationResult.builder()
                    .projectId(request.getProjectId())
                    .sceneNo(request.getSceneNo())
                    .safeInternalRef(minioPath)
                    .success(true)
                    .traceRef(buildTrace("IMAGE_GENERATION", "openai-dalle3"))
                    .needsHumanReview(true)
                    .build();

        } catch (Exception e) {
            log.error("gpt-image-1 görsel üretim hatası: projectId={}, sceneNo={}, error={}",
                    request.getProjectId(), request.getSceneNo(), e.getMessage());

            return TurkuImageGenerationResult.builder()
                    .projectId(request.getProjectId())
                    .sceneNo(request.getSceneNo())
                    .safeInternalRef("error-ref:" + UUID.randomUUID())
                    .success(false)
                    .safeErrorCode("IMAGE_GENERATION_FAILED")
                    .traceRef(buildTrace("IMAGE_GENERATION", "openai-gpt-image-1-error"))
                    .needsHumanReview(true)
                    .build();
        }
    }

    // ── DALL-E 3 API Call ────────────────────────────────────

    private byte[] callImageGeneration(TurkuImageGenerationRequest request) {
        // Prompt'u İngilizce olarak hazırla
        String prompt = buildDallE3Prompt(request);

        DallERequest requestBody = DallERequest.builder()
                .model("gpt-image-1")
                .prompt(prompt)
                .size("1536x1024")
                .quality("medium")
                .n(1)
                .build();

        try {
            log.info("gpt-image-1 çağrılıyor: sceneNo={}, promptLen={}", request.getSceneNo(), prompt.length());

            DallEResponse response = imageRestClient.post()
                    .uri("/images/generations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(DallEResponse.class);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                throw new RuntimeException("gpt-image-1 boş yanıt döndü");
            }

            DallEResponse.DallEImage imageData = response.getData().get(0);

            // gpt-image-1 b64_json döndürür
            if (imageData.getB64Json() != null && !imageData.getB64Json().isBlank()) {
                log.info("gpt-image-1 b64 yanıt: sceneNo={}, b64Len={}",
                        request.getSceneNo(), imageData.getB64Json().length());
                return java.util.Base64.getDecoder().decode(imageData.getB64Json());
            }

            // Fallback: URL döndüyse indir
            if (imageData.getUrl() != null && !imageData.getUrl().isBlank()) {
                log.info("gpt-image-1 URL yanıt: sceneNo={}", request.getSceneNo());
                return downloadImage(imageData.getUrl());
            }

            throw new RuntimeException("gpt-image-1 yanıtında ne b64 ne URL var");

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.error("gpt-image-1 HTTP {} hatası: {}", status, body);
            throw new RuntimeException("gpt-image-1 HTTP " + status + " hatası");
        } catch (ResourceAccessException e) {
            throw new RuntimeException("gpt-image-1 bağlantı/timeout hatası: " + e.getMessage());
        }
    }

    /**
     * Sahne prompt'unu DALL-E 3 için İngilizce'ye dönüştürür.
     * safePromptSummary Türkçe olabilir — İngilizce stil notları ile zenginleştirilir.
     */
    private String buildDallE3Prompt(TurkuImageGenerationRequest request) {
        StringBuilder sb = new StringBuilder();

        // Temel sahne açıklaması (İngilizce notlar ile sarma)
        sb.append("A cinematic, photorealistic scene for a documentary video about Turkish folk songs (türkü). ");
        sb.append("Scene ").append(request.getSceneNo()).append(": ");
        sb.append(request.getSafePromptSummary());

        // Stil ipucu varsa ekle
        if (request.getStyleHint() != null && !request.getStyleHint().isBlank()) {
            sb.append(". Style: ").append(request.getStyleHint());
        }

        // Negatif notlar — DALL-E 3'te prompt'un parçası olarak kullanılır
        if (request.getSafeNegativeNotes() != null && !request.getSafeNegativeNotes().isBlank()) {
            sb.append(". Avoid: ").append(request.getSafeNegativeNotes());
        }

        // Genel kalite notları
        sb.append(". High quality, cinematic lighting, Anatolian landscape, warm earthy tones, ");
        sb.append("documentary style, no text overlays, no watermarks.");

        return sb.toString();
    }

    // ── Image Download ──────────────────────────────────────

    private byte[] downloadImage(String imageUrl) {
        try {
            byte[] bytes = downloadRestClient.get()
                    .uri(URI.create(imageUrl))
                    .retrieve()
                    .body(byte[].class);

            if (bytes == null || bytes.length == 0) {
                throw new RuntimeException("İndirilen görsel boş");
            }

            log.debug("Görsel indirildi: size={}KB", bytes.length / 1024);
            return bytes;

        } catch (Exception e) {
            throw new RuntimeException("Görsel indirme hatası: " + e.getMessage(), e);
        }
    }

    // ── MinIO Upload ────────────────────────────────────────

    private String uploadToMinio(UUID projectId, int sceneNo, byte[] imageBytes) {
        String objectPath = String.format("turku-video/%s/scenes/%d.png", projectId, sceneNo);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectPath)
                    .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                    .contentType("image/png")
                    .build());

            log.info("MinIO'ya yüklendi: bucket={}, path={}", minioConfig.getBucketName(), objectPath);
            return objectPath;

        } catch (Exception e) {
            throw new RuntimeException("MinIO yükleme hatası: " + e.getMessage(), e);
        }
    }

    /**
     * MinIO'dan görsel okur. Controller tarafından kullanılır.
     */
    public byte[] getImageFromMinio(String objectPath) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectPath)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("MinIO okuma hatası: " + e.getMessage(), e);
        }
    }

    // ── System Prompt Builder ───────────────────────────────

    private String buildSystemPrompt(String stage) {
        return switch (stage) {
            case "STORY" -> """
                Sen bir Anadolu kültürü ve türkü tarihi profesörüsün, aynı zamanda ödüllü bir
                hikâye yazarısın. Görevin, verilen türkü hakkında derin, duygusal ve sinematik
                bir hikâye paketi hazırlamak. Bu hikâye 10 dakikalık profesyonel bir belgesel
                video için kullanılacak.

                KURALLAR:
                - Türkçe yaz — edebi, lirik, sinematik ve derinden duygusal bir dil kullan
                - Şarkı sözlerini (lyrics) YAZMA — telif hakkı ihlali
                - TOPLAM EN AZ 3000 kelime yaz
                - Hikâye bölümü EN AZ 1500 kelime — bu videonun kalbi
                - Karakterleri canlandır: isimler, duygular, iç çatışmalar, diyaloglar
                - Sahne sahne anlat — okuyucu orada gibi hissetsin
                - Beş duyuya hitap et: sesler, kokular, renkler, dokunuşlar
                - Dramatik yapı kur: tanışma → umut → engel → doruk → hüzünlü son
                - Okuyucu gözyaşlarını tutamamalı
                - Halk rivayetlerini ve gerçek bilgileri ayır

                FORMAT:
                ## Türkü Hakkında
                [Etkileyici tanıtım — 150-200 kelime]

                ## Tarihsel Bağlam
                [Dönem, coğrafya, sosyal ortam — 300-400 kelime.
                O dönemde Anadolu nasıl bir yerdi? İnsanlar nasıl yaşıyordu?]

                ## Hikâye
                [ANA HİKÂYE — EN AZ 1500 KELİME.
                5 bölümlü yapı:
                1. TANIŞMA (300 kelime): Kahraman, mekan, günlük yaşam
                2. AŞKIN DOĞUŞU (300 kelime): İlk karşılaşma, kalp çarpıntısı
                3. ENGELLER (300 kelime): Kavuşamama nedenleri, acı
                4. DUYGUSAL DORUK (300 kelime): Son karşılaşma veya veda
                5. MİRAS (300 kelime): Türkünün doğuşu ve yayılması
                Diyaloglar, iç monologlar, mekan betimlemeleri kullan.]

                ## Duygusal Katman
                [Neden bu kadar etkileyici — 250-300 kelime]

                ## Kültürel Miras
                [Bugün nasıl yaşıyor — 200-250 kelime]

                ## Kaynak Notu
                [Bilgi kaynakları ve güvenilirlik — 100 kelime]
                """;

            case "SCENARIO" -> """
                Sen bir YouTube içerik uzmanı ve senaryo yazarısın. Görevin, verilen türkü hikâyesi için
                10 dakikalık bir YouTube video senaryosu yazmak.

                KURALLAR:
                - Türkçe yaz
                - Şarkı sözlerini (lyrics) YAZMA
                - 6-beat yapısı kullan: Hook → Giriş → Hikâye → Duygusal Zirve → Kapanış → CTA
                - Her beat için: seslendirme metni (narration), süre, görsel açıklama, müzik notu yaz
                - Seslendirme metinleri uzun ve detaylı olsun — TTS ile seslendirilecek
                - İzleyiciyi duygusal olarak bağla — ilk 30 saniye kritik
                - 10 dakika hedefle (toplam süre)

                FORMAT (her beat için):
                ### Beat [numara]: [başlık]
                **Süre:** [başlangıç] - [bitiş]
                **Seslendirme:** [tam narration metni — TTS'e verilecek, EN AZ 3-4 cümle]
                **Görsel:** [sahne açıklaması]
                **Müzik:** [müzik atmosferi notu]
                **Duygu:** [hedeflenen duygu]
                """;

            case "STORYBOARD" -> """
                Sen profesyonel bir storyboard sanatçısısın. Görevin, verilen senaryo için
                10 dakikalık belgesel videonun sahne bazlı detaylı storyboard planını oluşturmak.

                KURALLAR:
                - Türkçe yaz — belgesel kalitesinde
                - 15-20 sahne planla (toplam 600 saniye = 10 dakika)
                - Her sahne EN AZ 30-45 saniye sürmeli
                - Her sahne seslendirmesi EN AZ 100-150 kelime — bu TTS ile seslendirilecek
                - Uzun, akıcı, duygusal cümleler yaz. Kısa kuru cümleler YAZMA
                - Görsel açıklamalar AI image prompt olarak kullanılacak — çok detaylı yaz
                - Sahneler arası akıcı geçiş sağla
                - Şarkı sözleri YAZMA

                FORMAT (her sahne için):
                ### Sahne [numara]
                **Süre:** [saniye cinsinden — toplam 600sn olmalı]
                **Seslendirme:** [TAM SESLENDİRME METNİ — EN AZ 100-150 kelime.
                Edebî, sinematik, duygusal. Belgesel anlatıcısı gibi yaz.
                TTS ile seslendirilecek — doğal akıcı cümleler kullan.]
                **Görsel:** [Detaylı görsel açıklama — renk, ışık, kompozisyon, atmosfer]
                **Mood:** [Duygusal atmosfer — açıklayıcı]
                **Kamera:** [Kamera açısı/hareketi]
                **Geçiş:** [Sonraki sahneye geçiş efekti]
                **Müzik:** [Müzik atmosferi ve enstrüman]
                """;

            case "PLANNING_BUNDLE" -> """
                Sen ödüllü bir belgesel yönetmeni, Anadolu kültürü profesörü ve en iyi hikâye
                yazarısın. Görevin, verilen türkü için TAM 10 DAKİKALIK profesyonel bir YouTube
                belgesel videosu üretim paketi hazırlamak.

                ÖNEMLİ KURALLAR:
                - Türkçe yaz — edebi, lirik, sinematik ve derinden duygusal bir dil kullan
                - Şarkı sözlerini (lyrics) ASLA YAZMA — telif hakkı ihlali
                - Bu bir profesyönel belgesel. Her kelime özenle seçilmeli
                - TOPLAM 4000-6000 kelime yaz — kısa yazma, bu 10 dakikalık video için içerik

                ═══════════════════════════════════════════
                # BÖLÜM 1: HİKÂYE ARAŞTIRMASI
                ═══════════════════════════════════════════

                ## Türkü Hakkında
                [Türkünün kısa ama etkileyici tanıtımı — 150-200 kelime.
                İlk cümle izleyiciyi çeksin. Neden bu türkü önemli?
                Hangi coğrafyada, ne zaman doğdu? Kimin eseri?]

                ## Tarihsel Bağlam
                [Dönemin detaylı resmi — 300-400 kelime.
                O dönemde Anadolu nasıl bir yerdi? İnsanlar nasıl yaşıyordu?
                Toplumsal koşullar, gelenekler, günlük hayat.
                Okuyucu o döneme ışınlansın.]

                ## Hikâye
                [═══ ANA HİKÂYE — EN AZ 1500 KELİME ═══
                Bu bölüm videonun KALBİDİR. Bir kısa roman gibi yazılmalı.

                YAPI:
                1. TANIŞMA (300 kelime): Kahramanı tanıt. Nereli, nasıl biri?
                   Günlük hayatını, çevresini, rüyalarını anlat.
                   Mekanı betimleme: kokular, renkler, sesler.

                2. AŞKIN DOĞUŞU (300 kelime): İlk karşılaşma anı.
                   Kalp çarpıntısı, utangaçlık, umut.
                   Diyaloglar ve iç monologlar kullan.
                   "O an dünya durdu" gibi anları detaylandır.

                3. ENGELLER (300 kelime): Neden kavuşamıyorlar?
                   Ailevi, toplumsal, coğrafi engeller.
                   Acı, özlem, bekleyiş.
                   Mektuplar, haberler, dedikodular.

                4. DUYGUSAL DORUK (300 kelime): En yoğun an.
                   Son karşılaşma veya hiç kavuşamama.
                   Gözyaşları, vedalaşma, kabulleniş.
                   İç dünya: "Sensiz bu dünya neye yarar?"

                5. MİRAS (300 kelime): Türkü nasıl doğdu?
                   Acıyı saza, sözlere dökmek.
                   Türkünün yayılışı, nesiller boyu aktarılması.
                   Bugün bile dinleyenleri ağlatan evrensel duygu.

                Her paragraf sinematik olsun — kamera açılarını hayal et.
                Beş duyuya hitap et: rüzgarın sesi, toprağın kokusu,
                gözyaşının sıcaklığı, sazın tınısı.
                Okuyucu AĞLAMALI.]

                ## Duygusal Katman
                [Bu türkü neden evrensel? — 250-300 kelime.
                Aşk, kayıp, özlem — bunlar herkesin duyguları.
                Dinleyici neden kendini bu hikâyede buluyor?]

                ## Kültürel Miras
                [Türkü bugün nasıl yaşıyor? — 200-250 kelime.
                Hangi sanatçılar seslendirdi? Festivallerde, düğünlerde,
                hasret gecelerinde nasıl yankılanıyor?]

                ═══════════════════════════════════════════
                # BÖLÜM 2: SENARYO (6-Beat, 10 Dakika)
                ═══════════════════════════════════════════

                ### Beat 1: Hook (0:00 - 0:30)
                **Seslendirme:** [Çarpıcı açılış — 3-4 güçlü cümle. İzleyiciyi anında yakala.]
                **Görsel:** [Dramatik açılış görseli]
                **Müzik:** [Hafif saz melodisi, gizemli]

                ### Beat 2: Giriş (0:30 - 2:00)
                **Seslendirme:** [Türkünün tanıtımı ve tarihsel bağlam — EN AZ 200 kelime]
                **Görsel:** [Anadolu manzaraları, dönem görselleri]
                **Müzik:** [Hafif fon müziği]

                ### Beat 3: Hikâye Bölüm 1 (2:00 - 4:30)
                **Seslendirme:** [Tanışma ve aşkın doğuşu — EN AZ 350 kelime]
                **Görsel:** [Karakterlerin yaşadığı mekanlar]
                **Müzik:** [Romantik, umut dolu]

                ### Beat 4: Hikâye Bölüm 2 — Duygusal Zirve (4:30 - 7:30)
                **Seslendirme:** [Engeller, acı, kavuşamama, doruk — EN AZ 450 kelime]
                **Görsel:** [Dramatik sahneler]
                **Müzik:** [Hüzünlü, yoğun]

                ### Beat 5: Kapanış (7:30 - 9:00)
                **Seslendirme:** [Türkünün mirası, bugünkü anlamı — EN AZ 200 kelime]
                **Görsel:** [Günümüz Anadolu'su]
                **Müzik:** [Nostaljik, huzurlu]

                ### Beat 6: CTA (9:00 - 10:00)
                **Seslendirme:** [Kapanış mesajı, kanal tanıtımı — 100 kelime]
                **Görsel:** [Logo, abone ol]
                **Müzik:** [Kapanış melodisi]

                ═══════════════════════════════════════════
                # BÖLÜM 3: STORYBOARD (15-20 Sahne)
                ═══════════════════════════════════════════

                Her sahne için AYRINTILI bilgi ver. Bu sahneler videonun iskeletidir.

                ### Sahne [N]
                **Süre:** [saniye cinsinden — toplam 600sn = 10dk olmalı]
                **Seslendirme:** [TAM SESLENDİRME METNİ — EN AZ 100-150 kelime.
                Bu metin TTS ile seslendirilecek. Uzun, akıcı, duygusal yaz.
                Kısa cümleler YAZMA. Edebî, belgesel tarzı anlatım kullan.
                Her sahne en az 30-45 saniye sürmeli.]
                **Görsel:** [Detaylı görsel açıklama — AI image prompt olarak kullanılacak.
                Renk paleti, kompozisyon, ışık, atmosfer belirt.]
                **Mood:** [Duygusal atmosfer — tek kelime değil, açıklayıcı]
                **Kamera:** [Kamera açısı ve hareketi]
                **Geçiş:** [Sonraki sahneye geçiş efekti]
                **Müzik:** [Müzik atmosferi ve enstrüman]
                """;

            default -> "Sen bir içerik uzmanısın. Verilen konu hakkında Türkçe detaylı bilgi üret.";
        };
    }

    // ── User Prompt Builder ─────────────────────────────────

    private String buildUserPrompt(String stage, TurkuNarrativeContext ctx, Map<String, String> params) {
        StringBuilder sb = new StringBuilder();

        // Temel bilgiler
        String turkuName = params != null ? params.getOrDefault("turkuName", "Bilinmeyen Türkü") : "Bilinmeyen Türkü";
        String region = params != null ? params.getOrDefault("region", "") : "";
        String mood = params != null ? params.getOrDefault("mood", "") : "";
        String sourceNotes = params != null ? params.getOrDefault("sourceNotes", "") : "";
        String narrationStyle = params != null ? params.getOrDefault("narrationStyle", "") : "";
        String visualStyle = params != null ? params.getOrDefault("visualStyle", "") : "";
        String targetDuration = params != null ? params.getOrDefault("targetDurationMinutes", "10") : "10";

        sb.append("## Türkü Bilgileri\n");
        sb.append("**Türkü Adı:** ").append(turkuName).append("\n");
        if (!region.isBlank()) sb.append("**Bölge:** ").append(region).append("\n");
        if (!mood.isBlank()) sb.append("**Atmosfer:** ").append(mood).append("\n");
        if (!sourceNotes.isBlank()) sb.append("**Kaynak Notları:** ").append(sourceNotes).append("\n");
        if (!narrationStyle.isBlank()) sb.append("**Anlatım Tarzı:** ").append(narrationStyle).append("\n");
        if (!visualStyle.isBlank()) sb.append("**Görsel Tarz:** ").append(visualStyle).append("\n");
        sb.append("**Hedef Video Süresi:** ").append(targetDuration).append(" dakika\n");

        // Narrative context (varsa)
        if (ctx != null) {
            sb.append("\n## Narratif Bağlam\n");
            if (ctx.getHookAngle() != null) sb.append("**Hook Açısı:** ").append(ctx.getHookAngle()).append("\n");
            if (ctx.getEmotionalArc() != null) sb.append("**Duygusal Yay:** ").append(ctx.getEmotionalArc()).append("\n");
            if (ctx.getToneGuide() != null) sb.append("**Ton Rehberi:** ").append(ctx.getToneGuide()).append("\n");
        }

        // Stage'e göre özel talimat
        sb.append("\n## Görev\n");
        sb.append(switch (stage) {
            case "STORY" -> "Bu türkü hakkında kapsamlı bir hikâye araştırması hazırla.";
            case "SCENARIO" -> "Bu türkünün hikâyesi için 6-beat YouTube video senaryosu yaz.";
            case "STORYBOARD" -> "Bu senaryo için detaylı sahne bazlı storyboard planı oluştur.";
            case "PLANNING_BUNDLE" -> "Bu türkü için tam üretim paketi hazırla: hikâye + senaryo + storyboard.";
            default -> "Bu konu hakkında detaylı içerik üret.";
        });

        return sb.toString();
    }

    // ── OpenAI HTTP Call ─────────────────────────────────────

    private String callOpenAi(String systemPrompt, String userPrompt, UUID projectId) {
        ChatCompletionRequest requestBody = ChatCompletionRequest.builder()
                .model(config.getModel())
                .messages(List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ))
                .temperature(config.getTemperature())
                .maxOutputTokens(config.getMaxOutputTokens())
                .build();

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("OpenAI boş yanıt döndü");
            }

            ChatCompletionResponse.Choice choice = response.getChoices().get(0);
            if (choice.getMessage() == null || choice.getMessage().getContent() == null) {
                throw new RuntimeException("OpenAI yanıtında content boş");
            }

            // Content filter kontrolü
            if ("content_filter".equals(choice.getFinishReason())) {
                throw new RuntimeException("OpenAI content filter — içerik reddedildi");
            }

            return choice.getMessage().getContent();

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            throw new RuntimeException("OpenAI HTTP " + status + " hatası");
        } catch (ResourceAccessException e) {
            throw new RuntimeException("OpenAI bağlantı/timeout hatası: " + e.getMessage());
        }
    }

    // ── Helper ──────────────────────────────────────────────

    private TurkuProviderTraceReference buildTrace(String stage, String provider) {
        return TurkuProviderTraceReference.builder()
                .traceId("openai-trace-" + UUID.randomUUID())
                .generationStage(stage)
                .providerAlias(provider)
                .timestamp(Instant.now().toString())
                .build();
    }

    // ── Internal DTOs ───────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class ChatCompletionRequest {
        private String model;
        private List<Map<String, String>> messages;
        private double temperature;
        @JsonProperty("max_completion_tokens")
        private int maxOutputTokens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatCompletionResponse {
        private List<Choice> choices;
        private Usage usage;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Choice {
            private Message message;
            @JsonProperty("finish_reason")
            private String finishReason;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Message {
            private String role;
            private String content;
            private String refusal;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Usage {
            @JsonProperty("prompt_tokens")
            private int promptTokens;
            @JsonProperty("completion_tokens")
            private int completionTokens;
            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }

    // ── DALL-E DTOs ─────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class DallERequest {
        private String model;
        private String prompt;
        private String size;
        private String quality;
        private int n;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DallEResponse {
        private long created;
        private List<DallEImage> data;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class DallEImage {
            private String url;
            @JsonProperty("b64_json")
            private String b64Json;
            @JsonProperty("revised_prompt")
            private String revisedPrompt;
        }
    }
}
