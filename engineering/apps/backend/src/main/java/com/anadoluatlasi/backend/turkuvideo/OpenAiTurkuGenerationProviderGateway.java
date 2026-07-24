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
            aiContent = callOpenAi(systemPrompt, userPrompt, request.getProjectId(), stage);
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
                Sen ödüllü bir belgesel film yönetmeni, Anadolu kültürü profesörü ve
                en iyi hikâye yazarısın. Görevin: verilen türkü hakkında bir SİNEMA FİLMİ
                tadında, baştan sona izleyiciyi içine çeken, gözyaşlarına boğan, derin
                ve geniş bir hikâye paketi hazırlamak.

                Bu hikâye 10 dakikalık profesyonel bir YouTube belgesel videosu için
                kullanılacak. Video görselleri bu hikâyeye göre üretilecek.

                ARAŞTIRMA KURALLARI:
                - Türkünün gerçek hikâyesini KAYNAKLARDAN araştır
                - Halk rivayetlerini, sözlü gelenek aktarımlarını, yerel anlatıları kullan
                - Birden fazla kaynak ve rivayet varsa en etkileyici olanı seç
                - Eğer kaynaklar yetersizse: türkünün SÖZLERİNDEN, MELODİSİNDEN ve
                  COĞRAFYASINDAN yola çıkarak uygun, inandırıcı, geniş bir hikâye kurgula
                - Gerçek bilgi ile kurguyu NET olarak belirt

                YAZIM KURALLARI:
                - Türkçe yaz — edebi, lirik, sinematik ve derinden duygusal bir dil kullan
                - Şarkı sözlerini (lyrics) YAZMA — telif hakkı ihlali
                - TOPLAM EN AZ 4000-5000 kelime yaz — bu bir film senaryosu gibi olmalı
                - Hikâye bölümü EN AZ 2500 KELİME — bu videonun KALBİ ve CAN DAMARI
                - Her sahneyi bir film yönetmeni gibi anlat: kamera açıları, ışık, renkler
                - Karakterleri CANLANDIR: fiziksel tasvirler, kişilik özellikleri, alışkanlıklar
                - İç monologlar ve diyaloglar kullan: "O an kalbi durdu..."
                - Beş duyuya hitap et: rüzgarın uğultusu, toprağın kokusu, sazın tınısı,
                  gözyaşının sıcaklığı, ekmeğin kokusu
                - Mekan betimlemelerini sinematik yap: "Güneş dağların ardından yükselirken,
                  sis çayırlardan usulca kalkıyordu..."
                - Okuyucu/izleyici AĞLAMALI — duygusal zirve çok güçlü olmalı
                - Dramatik yapı: TANIŞMA → UMUT → ENGEL → ÇATIŞMA → DORUK → VEDA → MİRAS

                FORMAT:
                ## Türkü Hakkında
                [Çarpıcı, etkileyici tanıtım — 200-300 kelime.
                İlk cümle izleyiciyi ANINDA yakalamalı.
                Bu türkü neden önemli? Neden yüzyıllardır söyleniyor?]

                ## Tarihsel Bağlam
                [Dönem, coğrafya, sosyal ortam — 400-500 kelime.
                O dönemde Anadolu nasıl bir yerdi? İnsanlar nasıl yaşıyordu?
                Köy hayatı, gelenekler, aile yapısı, toplumsal kurallar.
                Okuyucu kendini O DÖNEME IŞINLANMIŞ gibi hissetmeli.]

                ## Hikâye
                [═══ ANA HİKÂYE — EN AZ 2500 KELİME ═══
                Bu bir FİLM SENARYOSUdur. Baştan sona olay örgüsü ile devam eder.

                7 BÖLÜMLÜ YAPI:
                1. PROLOGUE (200 kelime): Atmosfer kurulumu. Mekan, zaman, hava.
                   "Bir sonbahar akşamı, dağlardan esen rüzgar..."

                2. TANIŞMA (400 kelime): Kahramanı tanıt.
                   Fiziksel tasvir, günlük hayatı, çevresi, hayalleri.
                   Nereli, ne iş yapıyor, ailesi nasıl?
                   Küçük detaylar: alışkanlıkları, sevdiği şeyler.

                3. KADER ANI (400 kelime): Hayatını değiştirecek an.
                   İlk karşılaşma veya olayın başlangıcı.
                   Zaman yavaşlasın, her detay büyüteçle anlatılsın.
                   Kalp çarpıntısı, titreme, dünya duruyor.

                4. UMUT ve MUTLULUK (300 kelime): Kısa ama yoğun mutlu dönem.
                   Gizli buluşmalar, mektuplar, bakışmalar.
                   Doğa ile paralel: bahar, çiçek açması.

                5. ENGELLER ve ÇATIŞMA (400 kelime): Acı başlıyor.
                   Ailevi, toplumsal, coğrafi engeller.
                   Zorla evlendirme, gurbet, savaş, hastalık...
                   İç dünya: çaresizlik, isyan, kabullenememe.

                6. DUYGUSAL DORUK (500 kelime): EN YOĞUN AN.
                   Son karşılaşma veya ebedi ayrılık.
                   Gözyaşları, vedalaşma, sessiz çığlık.
                   "Bir daha asla göremeyecekti..." gibi satırlar.
                   Bu bölüm izleyiciyi AĞLATMALI.

                7. MİRAS ve SONSÖZ (300 kelime): Acı, müziğe dönüşüyor.
                   Türkü nasıl doğdu? Kim söyledi ilk?
                   Nesiller boyu nasıl aktarıldı?
                   Bugün bile dinleyenlerin gözlerini nemlendiren evrensel duygu.

                Her paragraf SİNEMATİK olsun. Kamera açılarını hayal et.
                Mevsimler, günün saatleri, doğa olayları hikâyeyle paralel akmalı.]

                ## Duygusal Katman
                [Bu türkü neden evrensel? — 300-400 kelime.
                Aşk, kayıp, özlem, gurbet — herkesin duyguları.
                Dinleyici neden kendini bu hikâyede buluyor?
                Modern dünyada bile geçerliliği.]

                ## Kültürel Miras
                [Türkü bugün nasıl yaşıyor? — 250-300 kelime.
                Önemli sanatçılar, farklı yorumlar, festivaller.
                Genç nesile aktarımı. Dijital çağda türkü.]

                ## Kaynak Notu
                [Bilgi kaynakları ve güvenilirlik değerlendirmesi — 150 kelime.
                Gerçek bilgi / halk rivayeti / kurgu ayrımı net belirtilmeli.]

                ═══════════════════════════════════════════
                # KARAKTER ÇİZELGESİ (⚠️ İNGİLİZCE YAZ!)
                ═══════════════════════════════════════════

                ⚠️ Bu bölüm AI görsel üretim prompt'u olarak kullanılacak.
                Fal.ai modeli Türkçe ANLAMIYOR. Tüm fiziksel tanımları İNGİLİZCE yaz.
                Sadece isimler Türkçe kalabilir.

                ### Main Male Character
                **Name:** [hikâyedeki gerçek isim]
                **Age:** [age range fitting the story]
                **Physical:** [DETAILED — hair color/type, skin tone, facial features,
                build, facial hair. Example: "Dark wavy hair, wheat-colored skin,
                sharp nose, slightly furrowed brows, thin mustache, athletic build"]
                **Clothing:** [period-accurate Anatolian — vest, şalvar, sash, çarık.
                Example: "Dark brown wool vest over white linen shirt, loose gray
                şalvar pants, leather belt with small pouch, worn leather çarık boots"]
                **Distinctive:** [saz on back, ring, prayer beads, scar, etc.]

                ### Main Female Character
                **Name:** [hikâyedeki gerçek isim]
                **Age:** [age range]
                **Physical:** [DETAILED English description — hair, skin, eyes, face]
                **Clothing:** [period-accurate — entari, yemeni, jewelry.
                Example: "Burgundy embroidered entari dress, white yemeni headscarf
                loosely draped, silver coin necklace, henna on hands"]
                **Distinctive:** [defining object/accessory]

                ### Supporting Characters (minimum 2-3)
                For each:
                **Name/Role:** [e.g. Father, village ağa, best friend]
                **Age:** [estimated]
                **Physical:** [short but clear ENGLISH description]
                **Clothing:** [period and status appropriate]
                **Story role:** [why this character matters]
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
                Sen profesyonel bir storyboard sanatçısı ve sinema yönetmenisin.
                Görevin: verilen senaryo için 10 dakikalık belgesel videonun
                sahne bazlı DETAYLI storyboard planını oluşturmak.

                *** KRİTİK KURALLAR — UYULMASI ZORUNLU ***

                1. TAM 35 SAHNE YAZILACAK. 10 veya 15 sahne yazıp bırakmak YASAKTIR.
                   Sahne 1'den Sahne 35'e kadar HEPSINI yaz.

                2. BU BİR FİLM. Sessiz izleyen biri bile görsellerden hikâyeyi anlayabilmeli.
                   Her sahne hikâyenin bir ANINI gösterir — karakter portresi DEĞİL.

                3. YASAK GÖRSELLER:
                   - İki kişi yan yana durup kameraya bakan portre ❌
                   - Genel "güzel manzara" görseli ❌
                   - Aynı sahnenin tekrarı ❌

                4. ZORUNLU GÖRSELLER:
                   - Her sahne bir AKSİYON/AN gösterir: yürüme, saz çalma, ağlama, veda...
                   - Kamera açıları DEĞİŞKEN: close-up, wide, silüet, kuş bakışı...
                   - Yan karakterler (baba, arkadaş, ağa) kendi sahnelerinde görülmeli

                5. SESLENDIRME: Senaryo beat'lerindeki hikâyeyi sahnelere dağıt.
                   Her sahne 40-80 kelime. HİKÂYENİN O ANINI anlat, genel bilgi DEĞİL.

                FORMAT (her sahne için):
                ### Sahne [numara]
                **Süre:** [saniye — toplam 600sn = 10dk. Her sahne 15-20sn]
                **Seslendirme:** [TÜRKÇE — TTS ile seslendirilecek — 40-80 kelime.
                Hikâyenin BU ANINI anlat. Edebî, sinematik, duygusal.]
                **Görsel:** [⚠️ İNGİLİZCE YAZ — Bu metin AI image prompt olacak.
                Fal.ai modeli Türkçe ANLAMIYOR. İngilizce yaz!
                Format: "[Character] is [doing action] in [location].
                [Lighting/weather]. [Camera angle]. [Emotion]."
                Örnek: "Young man sitting at wooden table writing a letter by
                candlelight, old papers scattered around, deep thought on face,
                warm interior glow, close-up shot"
                ASLA iki kişi yan yana kameraya bakıyor YAZMA.]
                **Mood:** [Duygusal atmosfer]
                **Kamera:** [Kamera açısı — her sahnede DEĞİŞTİR]
                **Geçiş:** [Sonraki sahneye geçiş efekti]
                **Müzik:** [Müzik atmosferi]
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
                # BÖLÜM 1.5: KARAKTER ÇİZELGESİ (⚠️ İNGİLİZCE YAZ!)
                ═══════════════════════════════════════════

                ⚠️ KRİTİK: Bu bölüm AI görsel üretim prompt'u olarak DOĞRUDAN kullanılacak.
                Fal.ai modeli Türkçe ANLAMIYOR. Fiziksel tanımları İNGİLİZCE yaz.
                Sadece isimler Türkçe kalabilir.

                Film kadrosu oluştur — sadece 2 kişi değil, yan karakterler de!
                Yaş, dönem ve bölgeye uygun giyim. 19. yy Anadolu köylüsünde
                sarık/türban YOKTUR.

                ## Characters

                ### Main Male Character
                **Name:** [hikâyedeki gerçek isim]
                **Age:** [exact number — young love story = 20-25]
                **Physical:** [DETAILED: face shape, hair color/length/style,
                beard status (young = clean-shaven or light stubble), eye color,
                height, build. Example: "Dark wavy hair reaching collar, wheat-colored
                skin, strong jawline, light stubble, deep brown eyes, athletic build"]
                **Clothing:** [period-accurate with COLORS: "Dark brown wool vest over
                cream linen shirt, charcoal şalvar, red woven sash at waist,
                worn leather çarık boots"]
                **Distinctive:** [always carries saz on back, silver ring, etc.]
                **Expression:** [emotional, determined, melancholic, etc.]

                ### Main Female Character
                **Name:** [hikâyedeki gerçek isim]
                **Age:** [exact number]
                **Physical:** [DETAILED: face, long dark braided hair, eye color, etc.
                Example: "Long dark braided hair past waist, olive skin, almond-shaped
                green eyes, delicate face, slender build"]
                **Clothing:** [DETAILED with colors: "Deep burgundy embroidered entari,
                cream yemeni headscarf loosely draped, silver coin necklace,
                thin gold bangles, henna on hands"]
                **Expression:** [hopeful, longing, gentle, etc.]

                ### Supporting Character 1 (e.g. father, ağa, elder)
                **Name/Role:** [name and role]
                **Age:** [number]
                **Physical:** [short English description]
                **Clothing:** [short English description]

                ### Supporting Character 2 (e.g. friend, mother, rival)
                **Name/Role:** [name and role]
                **Age:** [number]
                **Physical:** [short English description]
                **Clothing:** [short English description]

                ### Supporting Character 3 (optional — if story requires)
                **Name/Role:** [name and role]
                **Age:** [number]
                **Physical:** [short English description]
                **Clothing:** [short English description]

                ### Setting and Period
                **Era:** [which century, which age]
                **Region:** [which city/region]
                **Location Details:** [village/town, house types (stone/wood/mudbrick),
                nature (mountain, plain, river)]
                **Season:** [season(s) in the story]
                **Color Palette:** [dominant colors for visuals]

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
                # BÖLÜM 3: STORYBOARD (TAM 35 SAHNE — EKSİK BIRAKMA)
                ═══════════════════════════════════════════

                *** KRİTİK KURALLAR — UYULMASI ZORUNLU ***

                1. TAM 35 SAHNE YAZILACAK. 10 veya 15 sahne yazıp bırakmak YASAKTIR.
                   Sahne 1'den Sahne 35'e kadar HEPSINI yaz.

                2. BU BİR FİLM. Sessiz izleyen biri bile görsellerden hikâyeyi anlayabilmeli.
                   Her sahne hikâyenin bir ANINI gösterir — karakter portresi DEĞİL.

                3. YASAK GÖRSELLER:
                   - İki kişi yan yana durup kameraya bakan portre ❌
                   - Genel "güzel manzara" görseli ❌
                   - Aynı sahnenin tekrarı ❌
                   - Karaktersiz boş mekan ❌
                   - Gitar veya Batı müzik aleti ❌ (SADECE bağlama/saz kullan)

                4. ZORUNLU GÖRSELLER:
                   - Her sahne bir AKSİYON/AN gösterir: yürüme, saz çalma, ağlama,
                     mektup yazma, at sürme, hasret, kavuşma, veda...
                   - Kamera açıları DEĞİŞKEN: close-up yüz, wide manzara, arkadan silüet,
                     kuş bakışı köy, omuz üstü, alt açı, yan profil...
                   - Her sahnede FARKLI mekan veya FARKLI an
                   - Yan karakterler (baba, arkadaş, ağa) kendi sahnelerinde görülmeli

                5. ⚠️ GÖRSEL PROMPT YAZIM KURALLARI (ÇOK ÖNEMLİ):
                   Görsel prompt'ta karakter FİZİKSEL TANIMI YAZMA!
                   Saç rengi, sakal, kıyafet detayı, yaş, göz rengi YAZMA!
                   Bunlar zaten karakter çizelgesinden otomatik eklenecek.
                   Sadece şunları yaz:
                   - KARAKTERİN İSMİ (Sümmani, Mihriban vb.)
                   - AKSİYON (ne yapıyor: walking, playing saz, crying, writing)
                   - MEKAN (where: village square, mountain path, river bank)
                   - IŞIK/HAVA (golden sunset, moonlight, misty morning)
                   - CİNSİYET BELİRT: "man" veya "woman" diye açıkça belirt

                   ✅ DOĞRU: "Sümmani (man) walking alone through snowy mountain path,
                      carrying a bağlama on his back, misty mountains, low winter sun,
                      wide establishing shot, deep loneliness"
                   ✅ DOĞRU: "Sümmani (man) and Mihriban (woman) meeting at the village
                      fountain, spring flowers blooming, shy first glance, medium shot"
                   ✅ DOĞRU: "Mihriban (woman) sitting by the window, embroidering,
                      rain outside, warm oil lamp light inside, melancholic expression"
                   ❌ YANLIŞ: "Young handsome man with dark curly hair wearing brown vest"
                   ❌ YANLIŞ: "Beautiful woman with long flowing hair in red dress"
                   ❌ YANLIŞ: "Two people standing together" (erkek/kadın BELİRT!)

                6. ENSTRÜMAN KURALI:
                   Müzik aleti = SADECE "bağlama" veya "saz" yaz.
                   ASLA "guitar", "lute", "oud" yazma.
                   Bağlama açıklaması: long thin neck, small pear-shaped wooden body.

                7. HİKÂYE KRONOLOJİSİ (⚠️ EN KRİTİK KURAL):
                   35 sahneden EN AZ 30 TANESİ hikâyenin kendisini anlatmalı!
                   Genel tarih, kültür, miras gibi dolgu sahne YASAK.

                   Sahne 1-2: ATMOSFER — Mekan tanıtımı (köy, dağ, ırmak).
                     Seslendirme: dönemin ve coğrafyanın kısa betimlemesi.
                   Sahne 3-7: TANIŞMA — Ana karakterin tanıtımı, günlük yaşamı,
                     ailesi, çevresi, hayalleri. Her sahne farklı bir detay.
                   Sahne 8-12: KARŞILAŞMA — İlk bakış, ilk söz, ilk heyecan.
                     Aşkın filizlenmesi. Utangaç anlar, çarpan kalpler.
                   Sahne 13-17: MUTLULUK — Gizli buluşmalar, mektuplar, bakışmalar.
                     Birlikte geçen anlar. Doğa ile paralel: bahar, çiçek.
                   Sahne 18-22: ENGELLER — Baba/ağa karşı çıkıyor. Toplumsal baskı.
                     Zorla evlendirme tehdidi. Gurbet zorunluluğu.
                     Her sahnede farklı bir engel veya çatışma anı.
                   Sahne 23-27: AYRILIK VE ACI — Veda anı, göç yolu, yalnızlık.
                     Geride kalanın acısı, gidenin hasreti.
                     İç monologlar, sessiz gözyaşları, uykusuz geceler.
                   Sahne 28-30: DUYGUSAL DORUK — En yoğun sahneler.
                     Son mektup, son hayal kırıklığı veya ebedi ayrılık.
                     Türkünün doğduğu AN — saz elde, acı sözlere dönüşüyor.
                   Sahne 31-32: MİRAS — Türkü köyden köye yayılıyor.
                     Nesiller boyu aktarılıyor.
                   Sahne 33-35: KAPANIŞ — Türkü bugün hâlâ söyleniyor.
                     Evrensel duygu, izleyiciye son mesaj, CTA.

                   ⚠️ YASAK: Sahne 12'den sonra "tarihsel bağlam", "kültürel önem",
                   "Anadolu geleneği" gibi GENEL BİLGİ sahnesi yazmak!
                   Her sahne MUTLAKA hikâyenin bir OLAYINI, bir ANINI göstermeli.
                   "Gelenekler", "toplumsal yapı" gibi bilgiler hikâye İÇİNDE
                   karakter diyalogları ve aksiyonlarla verilmeli.

                   ⚠️ TEKRAR YASAĞI: Hiçbir iki sahne aynı eylemi, aynı mekanı
                   ve aynı duyguyu TEKRAR EDEMEZ. Her sahne BENZERSİZ olmalı.

                8. SESLENDİRME: Senaryodaki beat'lerin hikâye anlatımını sahnelere dağıt.
                   Her sahne 40-80 kelime seslendirme. Genel tarihî bilgi DEĞİL,
                   hikâyenin o anını anlat: "O akşam sazını eline aldığında..."

                9. ⚠️ SAYI KURALI (ÇOK ÖNEMLİ — TTS İÇİN):
                   Seslendirme metninde RAKAM KULLANMA! Tüm sayıları YAZIYYLA yaz.
                   Bu metin TTS (text-to-speech) ile seslendirilecek, rakamlar yanlış okunur.
                   ✅ DOĞRU: "on dokuzuncu yüzyıl", "bin sekiz yüz altmış yedi"
                   ❌ YANLIŞ: "19. yüzyıl", "1867"
                   ✅ DOĞRU: "yirmi beş yaşında", "üç gün sonra", "beş yıl boyunca"
                   ❌ YANLIŞ: "25 yaşında", "3 gün sonra", "5 yıl boyunca"
                   Bu kural HİKÂYE ve SENARYO bölümlerindeki tüm metinler için de geçerlidir.

                ### Sahne [N]
                **Süre:** [saniye — toplam 600sn = 10dk. Her sahne 15-20sn]
                **Karakterler:** [Bu sahnede kimler var? İsim + cinsiyet belirt:
                "Sümmani (erkek)" veya "Sümmani (erkek) ve Mihriban (kadın)"]
                **Seslendirme:** [TÜRKÇE — TTS ile seslendirilecek metin — 40-80 kelime.
                Hikâyenin BU ANINI anlat. Edebî, sinematik, duygusal.
                ⚠️ RAKAM KULLANMA — sayıları yazıyla yaz: "on dokuzuncu yüzyıl" gibi.]
                **Görsel:** [⚠️ İNGİLİZCE YAZ — Bu metin direkt AI image prompt olacak.
                SADECE AKSİYON + MEKAN + IŞIK + KAMERA yaz.
                Karakter fiziksel tanımı YAZMA (otomatik eklenecek).
                Karakter isimlerini ve cinsiyetini (man/woman) belirt.
                Müzik aleti = SADECE "bağlama" yaz, ASLA "guitar" yazma.]
                **Mood:** [Duygusal atmosfer]
                **Kamera:** [Kamera açısı — her sahnede DEĞİŞTİR]
                **Geçiş:** [Sonraki sahneye geçiş efekti]
                **Müzik:** [Müzik atmosferi]
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

        // ═══ ÖNCEKİ STAGE ÇIKTILARI — ZİNCİRLEME BAĞLAM ═══
        // Bu sayede GPT, önceki adımlarda ürettiği hikâye ve senaryoyu BILEREK
        // sonraki adımda tutarlı narration üretebilir.

        String previousStory = params != null ? params.getOrDefault("previousStoryOutput", "") : "";
        String previousScenario = params != null ? params.getOrDefault("previousScenarioOutput", "") : "";
        String characterSheet = params != null ? params.getOrDefault("characterSheet", "") : "";

        // SCENARIO adımı → hikâyeyi ver
        if ("SCENARIO".equals(stage) && !previousStory.isBlank()) {
            sb.append("\n═══════════════════════════════════════════\n");
            sb.append("## ÖNCEKİ ADIMDA ÜRETİLEN HİKÂYE\n");
            sb.append("Aşağıdaki hikâyeyi SENARYO'ya dönüştür. Hikâyedeki karakterleri,\n");
            sb.append("olayları, duyguları ve kronolojik sırayı KORU. Generic cümleler YAZMA.\n");
            sb.append("═══════════════════════════════════════════\n\n");
            sb.append(previousStory).append("\n\n");

            if (!characterSheet.isBlank()) {
                sb.append("## KARAKTER ÇİZELGESİ\n");
                sb.append(characterSheet).append("\n\n");
            }
        }

        // STORYBOARD adımı → hem hikâye hem senaryo ver
        if ("STORYBOARD".equals(stage)) {
            if (!previousStory.isBlank()) {
                sb.append("\n═══════════════════════════════════════════\n");
                sb.append("## ÖNCEKİ ADIMDA ÜRETİLEN HİKÂYE\n");
                sb.append("Bu hikâyeyi 35 sahneye bölerken HİKÂYENİN KENDİSİNİ kullan.\n");
                sb.append("Her sahnenin seslendirmesi hikâyenin O ANINI anlatmalı.\n");
                sb.append("═══════════════════════════════════════════\n\n");
                sb.append(previousStory).append("\n\n");
            }

            if (!characterSheet.isBlank()) {
                sb.append("## KARAKTER ÇİZELGESİ\n");
                sb.append(characterSheet).append("\n\n");
            }

            if (!previousScenario.isBlank()) {
                sb.append("═══════════════════════════════════════════\n");
                sb.append("## ÖNCEKİ ADIMDA ÜRETİLEN SENARYO\n");
                sb.append("Bu senaryo beat'lerini 35 sahneye dağıt. Beat sırasını koru.\n");
                sb.append("═══════════════════════════════════════════\n\n");
                sb.append(previousScenario).append("\n\n");
            }
        }

        // Stage'e göre özel talimat
        sb.append("\n## Görev\n");
        sb.append(switch (stage) {
            case "STORY" -> "Bu türkü hakkında kapsamlı bir hikâye araştırması hazırla.";
            case "SCENARIO" -> "Yukarıdaki hikâyeyi temel alarak 6-beat YouTube video senaryosu yaz. " +
                    "Hikâyedeki karakter isimlerini, olayları ve duyguları AYNEN kullan. " +
                    "Her beat'in seslendirme metni hikâyenin İLGİLİ BÖLÜMÜNÜ anlatmalı — generic olma!";
            case "STORYBOARD" -> "Yukarıdaki hikâye ve senaryoyu temel alarak 35 sahnelik detaylı storyboard planı oluştur. " +
                    "Her sahnenin seslendirmesi hikâyenin O ANINI anlatmalı. " +
                    "Karakter isimleri, mekânlar ve duygular hikâyeye SADIK kalmalı.";
            case "PLANNING_BUNDLE" -> "Bu türkü için tam üretim paketi hazırla: hikâye + senaryo + storyboard.";
            default -> "Bu konu hakkında detaylı içerik üret.";
        });

        return sb.toString();
    }

    // ── OpenAI HTTP Call ─────────────────────────────────────

    private String callOpenAi(String systemPrompt, String userPrompt, UUID projectId) {
        return callOpenAi(systemPrompt, userPrompt, projectId, "UNKNOWN");
    }

    private String callOpenAi(String systemPrompt, String userPrompt, UUID projectId, String stage) {
        ChatCompletionRequest requestBody = ChatCompletionRequest.builder()
                .model(config.getModel())
                .messages(List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ))
                .temperature(config.getTemperature())
                .maxOutputTokens(config.getMaxOutputTokens())
                .build();

        log.info("🤖 OpenAI çağrısı başlatılıyor: stage={}, model={}, maxTokens={}, systemPromptLen={}, userPromptLen={}",
                stage, config.getModel(), config.getMaxOutputTokens(),
                systemPrompt.length(), userPrompt.length());

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

            // ── TOKEN KULLANIM TAKİBİ (Bütçe hesaplaması için) ──
            if (response.getUsage() != null) {
                var usage = response.getUsage();
                log.info("💰 TOKEN KULLANIMI [{}] | model={} | prompt={} | completion={} | total={} | projectId={}",
                        stage, config.getModel(),
                        usage.getPromptTokens(), usage.getCompletionTokens(),
                        usage.getTotalTokens(), projectId);
            }

            // Content filter kontrolü
            if ("content_filter".equals(choice.getFinishReason())) {
                throw new RuntimeException("OpenAI content filter — içerik reddedildi");
            }

            // Token limiti kontrolü — "length" = yanıt kesildi!
            if ("length".equals(choice.getFinishReason())) {
                log.warn("⚠️ [{}] OpenAI yanıtı TOKEN LİMİTİ nedeniyle KESİLDİ! " +
                        "maxOutputTokens={}, completionTokens={}, kesilen metin: {} karakter",
                        stage, config.getMaxOutputTokens(),
                        response.getUsage() != null ? response.getUsage().getCompletionTokens() : "?",
                        choice.getMessage().getContent().length());
            }

            String content = choice.getMessage().getContent();
            log.info("✅ [{}] OpenAI yanıt alındı: finishReason={}, contentLen={} karakter",
                    stage, choice.getFinishReason(), content.length());

            return content;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.error("❌ [{}] OpenAI HTTP {} hatası: {}", stage, status, e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI HTTP " + status + " hatası");
        } catch (ResourceAccessException e) {
            log.error("❌ [{}] OpenAI bağlantı/timeout hatası: {}", stage, e.getMessage());
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
