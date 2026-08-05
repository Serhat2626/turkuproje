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
                tadında, baştan sona izleyiciyi içine çeken, derinden etkileyen, derin
                ve geniş bir hikâye paketi hazırlamak.

                Bu hikâye 10 dakikalık profesyonel bir YouTube belgesel videosu için
                kullanılacak. Video görselleri bu hikâyeye göre üretilecek.

                ⚠️ ÖNEMLİ: Türküler farklı temalarda olabilir. Önce türkünün temasını belirle:
                - AŞK HİKÂYESİ (kavuşamama, hasret, platonik aşk)
                - SAVAŞ / KAHRAMANLIK (Çanakkale, cephe, şehitlik)
                - GURBET (göç, memleket hasreti, gurbetçi acısı)
                - AĞIT (ölüm, yas, kayıp acısı)
                - DOĞA / YÖRE (dağlar, ovalar, mevsimler, köy hayatı)
                - İSYAN / DİRENİŞ (eşkıyalık, zulme karşı durma)
                - ANNELİK / AİLE (evlat hasreti, ana-baba sevgisi)
                - TARİHSEL OLAY (deprem, kıtlık, göç, sürgün)
                Hikâye yapısını TEMAYA GÖRE uyarla. Her türküyü aşk hikâyesi gibi yazma!

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
                - İç monologlar ve diyaloglar kullan
                - Beş duyuya hitap et: rüzgarın uğultusu, toprağın kokusu, sazın tınısı,
                  gözyaşının sıcaklığı, ekmeğin kokusu
                - Mekan betimlemelerini sinematik yap
                - İzleyici derinden ETKİLENMELİ — duygusal zirve çok güçlü olmalı

                FORMAT:
                ## Türkü Hakkında
                [Çarpıcı, etkileyici tanıtım — 200-300 kelime.
                İlk cümle izleyiciyi ANINDA yakalamalı.
                Bu türkü neden önemli? Neden yüzyıllardır söyleniyor?
                Teması ne: aşk mı, savaş mı, gurbet mi, ağıt mı?]

                ## Tarihsel Bağlam
                [Dönem, coğrafya, sosyal ortam — 400-500 kelime.
                O dönemde Anadolu nasıl bir yerdi? İnsanlar nasıl yaşıyordu?
                Türkünün temasına göre bağlam kur:
                - Aşk türküsü → köy hayatı, gelenekler, aile yapısı
                - Savaş türküsü → cephe koşulları, askere uğurlama, vatan sevgisi
                - Gurbet türküsü → göç nedenleri, geride bırakılanlar, yol zorluğu
                - Ağıt → ölüm koşulları, yas gelenekleri, toplumsal acı
                Okuyucu kendini O DÖNEME IŞINLANMIŞ gibi hissetmeli.]

                ## Hikâye
                [═══ ANA HİKÂYE — EN AZ 2500 KELİME ═══
                Bu bir FİLM SENARYOSUdur. Baştan sona olay örgüsü ile devam eder.

                7 BÖLÜMLÜ EVRENSEL YAPI (temaya göre uyarla):
                1. PROLOGUE (200 kelime): Atmosfer kurulumu. Mekan, zaman, hava.
                   Hikâyenin geçtiği dünyayı gözler önüne ser.

                2. TANIŞMA (400 kelime): Ana kahramanı tanıt.
                   Fiziksel tasvir, günlük hayatı, çevresi, hayalleri/korkuları.
                   Nereli, ne iş yapıyor, ailesi nasıl?
                   Küçük detaylar: alışkanlıkları, sevdiği şeyler.

                3. KADER ANI (400 kelime): Hayatını değiştirecek olay/kişi/an.
                   Bu AN temaya göre değişir:
                   - Aşk → ilk karşılaşma, ilk bakış
                   - Savaş → seferberlik haberi, veda günü
                   - Gurbet → göç kararı, yola çıkış
                   - Ağıt → kaybın gerçekleştiği an
                   - İsyan → zulmün doruk noktası
                   Zaman yavaşlasın, her detay büyüteçle anlatılsın.

                4. UMUT veya DİRENİŞ (300 kelime): Kısa ama yoğun olumlu dönem.
                   - Aşk → gizli buluşmalar, mutlu anlar
                   - Savaş → zafer umudu, silah arkadaşlığı
                   - Gurbet → yeni diyarda tutunma çabası
                   - İsyan → halk desteği, küçük zaferler

                5. ENGELLER ve ÇATIŞMA (400 kelime): Acı/zorluk başlıyor.
                   Temaya göre: ailevi engeller, düşman baskısı, hastalık,
                   yalnızlık, ihanet, doğa felaketleri...
                   İç dünya: çaresizlik, isyan, kabullenememe.

                6. DUYGUSAL DORUK (500 kelime): EN YOĞUN AN.
                   - Aşk → ebedi ayrılık, son veda
                   - Savaş → şehitlik, son mektup, zafer/yenilgi anı
                   - Gurbet → memlekete dönememe, geride kalanın ölüm haberi
                   - Ağıt → kabulleniş, son bakış, toprak kokusu
                   Bu bölüm izleyiciyi DERİNDEN ETKİLEMELİ.

                7. MİRAS ve SONSÖZ (300 kelime): Acı/deneyim, müziğe dönüşüyor.
                   Türkü nasıl doğdu? Kim söyledi ilk?
                   Nesiller boyu nasıl aktarıldı?
                   Bugün bile dinleyenleri etkileyen evrensel duygu.

                Her paragraf SİNEMATİK olsun. Kamera açılarını hayal et.
                Mevsimler, günün saatleri, doğa olayları hikâyeyle paralel akmalı.]

                ## Duygusal Katman
                [Bu türkü neden evrensel? — 300-400 kelime.
                Hangi temel insani duyguya dokunuyor?
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

                ⚠️ Karakter sayısı ve türü TEMAYA GÖRE DEĞİŞİR:
                - Aşk → erkek + kadın + aile/toplum karakterleri
                - Savaş → asker(ler) + komutan + geride kalan aile
                - Gurbet → gurbetçi + geride bıraktıkları
                - Ağıt → kaybedilen kişi + yasını tutan(lar)
                - İsyan → isyancı + zalim + halk

                ### Main Character 1
                **Name:** [hikâyedeki gerçek isim]
                **Role:** [kahramanın rolü: âşık, asker, gurbetçi, ana, eşkıya vs.]
                **Age:** [age range fitting the story]
                **Gender:** [male/female]
                **Physical:** [DETAILED — hair color/type, skin tone, facial features,
                build. Period and role appropriate.]
                **Clothing:** [period-accurate Anatolian clothing, role appropriate.
                Asker ise: üniforma/cephe kıyafeti. Köylü ise: şalvar, yelek, çarık.]
                **Distinctive:** [defining object: saz, tüfek, orak, tespih, mektup vs.]

                ### Main Character 2 (if applicable)
                **Name:** [hikâyedeki gerçek isim]
                **Role:** [sevgili, eş, anne, silah arkadaşı vs.]
                **Age:** [age range]
                **Gender:** [male/female]
                **Physical:** [DETAILED English description]
                **Clothing:** [period-accurate, role appropriate]
                **Distinctive:** [defining object/accessory]

                ### Supporting Characters (minimum 2-3)
                For each:
                **Name/Role:** [e.g. Father, commander, village elder, friend]
                **Age:** [estimated]
                **Gender:** [male/female]
                **Physical:** [short but clear ENGLISH description]
                **Clothing:** [period, status and role appropriate]
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

                ⚠️ Türkünün temasını belirle (aşk, savaş, gurbet, ağıt, isyan,
                doğa, annelik, tarihsel olay) ve yapıyı TEMAYA GÖRE uyarla.

                YAPI:
                1. TANIŞMA (300 kelime): Kahramanı tanıt. Nereli, nasıl biri?
                   Günlük hayatını, çevresini, hayallerini/korkularını anlat.
                   Mekanı betimleme: kokular, renkler, sesler.

                2. KADER ANI (300 kelime): Hayatı değiştiren olay/kişi/an.
                   Temaya göre: ilk karşılaşma / savaş haberi / göç kararı /
                   kaybın başlangıcı / zulmün doruk noktası.
                   Diyaloglar ve iç monologlar kullan.

                3. ENGELLER (300 kelime): Zorluklar derinleşiyor.
                   Temaya göre: ailevi engeller / düşman baskısı / gurbet
                   yalnızlığı / hastalık / ihanet.
                   Acı, özlem, bekleyiş, mücadele.

                4. DUYGUSAL DORUK (300 kelime): En yoğun an.
                   Temaya göre: ebedi ayrılık / şehitlik / dönememe /
                   kabulleniş / son direniş.
                   Bu bölüm izleyiciyi DERİNDEN ETKİLEMELİ.

                5. MİRAS (300 kelime): Acı/deneyim müziğe dönüşüyor.
                   Türkü nasıl doğdu? Kim söyledi ilk?
                   Türkünün yayılışı, nesiller boyu aktarılması.
                   Bugün bile dinleyenleri etkileyen evrensel duygu.

                Her paragraf sinematik olsun — kamera açılarını hayal et.
                Beş duyuya hitap et: rüzgarın sesi, toprağın kokusu,
                gözyaşının sıcaklığı, sazın tınısı.
                İzleyici DERİNDEN ETKİLENMELİ.]

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
                # BÖLÜM 3: STORYBOARD (FİLM ROMANI YAKLAŞIMI)
                ═══════════════════════════════════════════

                *** TEMEL PRENSİP — BUNU HER ŞEYİN ÜSTÜNDE TUT ***

                Bu storyboard bir FİLM ROMANI adaptasyonudur.
                Senin görevin hikâyeyi YENİDEN YAZMAK DEĞİL,
                hikâye metnini OLDUĞU GİBİ sahnelere bölmek ve
                her sahne için uygun görsel prompt üretmektir.

                SESLENDİRME = HİKÂYE METNİNİN KENDİSİ.
                Hikâye metnindeki cümleler, TTS ile seslendirilecek.
                Sen yeni cümle UYDURMA, mevcut metni BÖLERSIN.

                *** STORYBOARD OLUŞTURMA ADIMLARI ***

                ADIM 1: Hikâye metnini temizle
                  - Kamera yönlendirmeleri çıkar ("Kameranın geniş açıyla...")
                  - Sahne teknik bilgileri çıkar ("Sabah ışıkları...")
                  - Sadece ANLATIM METNİNİ bırak (seslendirilecek kısım)

                ADIM 2: Temizlenmiş metni bölümlere ayır
                  Bölüm dağılımı:

                  A) TARİHSEL BAĞLAM bölümü → 2-3 sahne
                     Bu bölümdeki 3 paragrafı 2-3 sahneye böl.
                     Her sahne 40-80 kelime seslendirme.
                     Görseller: Genel Anadolu/Erzurum atmosferi.
                     NOT: Bu bölüm OLDUĞU GİBİ seslendirilecek.

                  B) HİKÂYE bölümü (Prologue'dan Sonsöz'e) → 28-30 sahne
                     Hikâyedeki HER CÜMLEYİ sahne seslendirmelerine dağıt.
                     Her sahne 2-4 cümle (40-80 kelime).
                     Hiçbir cümle ATLANMAMALIdır.
                     Hikâye metninin TAMAMI seslendirilmelidir.

                  C) KAPANIŞ → 2-3 sahne
                     Miras/Sonsöz bölümü + CTA.
                     Bu kısım kısa tutulmalı.

                  TOPLAM: 33-35 sahne

                ADIM 3: Her sahne için görsel prompt üret
                  Seslendirme metnine UYGUN bir görsel prompt yaz.
                  Görsel, o anda anlatılan OLAYI veya DUYGUYU göstermeli.

                *** SAHNELERİN YAZIM KURALLARI ***

                1. SESLENDİRME KURALI (EN KRİTİK):
                   ⛔ KENDİ CÜMLENİ YAZMA! Hikâye metnindeki cümleleri kullan.
                   ⛔ Hikâyeyi ÖZETLEME! Her cümle bir sahnede yer almalı.
                   ⛔ "Bu türkü yıllardır söylenir" gibi GENEL ifade ekleme.
                   ✅ Hikâye metninden 2-4 cümleyi al, sahneye yerleştir.
                   ✅ Gerekirse küçük bağlaç düzeltmeleri yap (ör: "Bu dönemde"→"O dönemde")
                   ✅ Rakamları yazıyla yaz: "19. yüzyıl" → "on dokuzuncu yüzyıl"

                2. GÖRSEL PROMPT KURALLARI:
                   ⚠️ İNGİLİZCE YAZ — AI image generation prompt'u olacak.
                   STİL: Digital illustration, graphic novel art style.
                   Karakter FİZİKSEL TANIMI YAZMA (otomatik eklenecek).
                   SADECE şunları yaz:
                   - Karakter İSMİ + CİNSİYET: "Sümmani (man)", "Mihriban (woman)"
                   - AKSİYON: ne yapıyor (walking, playing bağlama, crying)
                   - MEKAN: nerede (village square, mountain path, window)
                   - IŞIK/HAVA: (golden sunset, moonlight, snowy morning)
                   - KAMERA: (wide shot, close-up, medium shot)
                   - DUYGU: (sorrow, longing, hope, anger)
                   Müzik aleti = SADECE "bağlama" yaz, ASLA "guitar" yazma.

                   ✅ DOĞRU: "Sümmani (man) playing bağlama alone in snowy village
                      square at dawn, smoke rising from chimneys, wide shot,
                      deep sorrow, graphic novel illustration"
                   ❌ YANLIŞ: "Young man with dark hair holding guitar"

                3. HİKÂYE AKIŞ KONTROLÜ:
                   Her sahne bir FARKLI AN göstermeli.
                   İki sahne aynı mekanı, aynı eylemi gösteremez.
                   Kamera açıları her sahnede DEĞİŞMELİ:
                   wide → close-up → medium → silhouette → bird's eye → over-shoulder

                4. SAYI KURALI (TTS İÇİN):
                   Seslendirme metninde RAKAM KULLANMA!
                   ✅ "on dokuzuncu yüzyıl", "bin sekiz yüz altmış yedi"
                   ❌ "19. yüzyıl", "1867"

                *** ÖRNEKLEMİZ — MİHRİBAN HİKÂYESİ İÇİN ***

                Hikâye metni şöyle başlıyorsa:
                "Erzurum'un karlı tepelerinde, eski bir köyün ortasındaki küçük
                meydanda, kışın keskin soğuğu iliklere kadar işler."

                Sahne seslendirmesi AYNEN BU OLACAK:
                "Erzurum'un karlı tepelerinde, eski bir köyün ortasındaki küçük
                meydanda, kışın keskin soğuğu iliklere kadar işler."

                ⛔ YANLIŞ: "Erzurum'da bir kış günü, soğuk bir köyde
                hikâyemiz başlıyor." (BU KENDİ CÜMLENDİR, YASAK!)

                *** SAHNE FORMATI ***

                ### Sahne [N]
                **Süre:** [saniye — toplam 600sn = 10dk. Her sahne 15-20sn]
                **Karakterler:** [Bu sahnede kimler var? İsim + cinsiyet belirt:
                "Sümmani (erkek)" veya "Sümmani (erkek) ve Mihriban (kadın)"
                Karakter yoksa "Yok — manzara/atmosfer" yaz]
                **Seslendirme:** [⚠️ HİKÂYE METNİNDEN ALINMIŞ 2-4 CÜMLE.
                Kendi cümleni YAZMA. Hikâye metnini AYNEN kullan.
                Sadece rakamları yazıyla yaz ve küçük bağlaç düzeltmeleri yap.
                40-80 kelime arası olmalı.]
                **Görsel:** [⚠️ İNGİLİZCE — AI image prompt.
                Seslendirmedeki ANI görselleştir.
                Karakter ismi + cinsiyet + aksiyon + mekan + ışık + kamera.]
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

        // STORYBOARD adımı → SADECE hikâyeyi ver (senaryo GÖNDERME — özetlemeye teşvik eder!)
        if ("STORYBOARD".equals(stage)) {
            if (!previousStory.isBlank()) {
                sb.append("\n═══════════════════════════════════════════\n");
                sb.append("## SESLENDİRME KAYNAK METNİ (HİKÂYE)\n");
                sb.append("⚠️ KRİTİK: Aşağıdaki metin seslendirilecek KAYNAK METİNDİR.\n");
                sb.append("Bu metnin HER CÜMLESİ sahnelere dağıtılmalı, HİÇBİR CÜMLE ATLANMAMALI.\n");
                sb.append("Sen yeni cümle UYDURMA — bu metnin cümlelerini OLDUĞU GİBİ kullan.\n");
                sb.append("Sadece rakamları yazıyla yaz ve küçük bağlaç düzeltmeleri yap.\n");
                sb.append("═══════════════════════════════════════════\n\n");
                sb.append(previousStory).append("\n\n");
            }

            if (!characterSheet.isBlank()) {
                sb.append("## KARAKTER ÇİZELGESİ\n");
                sb.append(characterSheet).append("\n\n");
            }

            // ⛔ SENARYO GÖNDERİLMİYOR — senaryo beat'leri özet olduğu için
            // AI'ın kısayol alıp hikâyeyi atlamasına neden oluyordu.
        }

        // Stage'e göre özel talimat
        sb.append("\n## Görev\n");
        sb.append(switch (stage) {
            case "STORY" -> "Bu türkü hakkında kapsamlı bir hikâye araştırması hazırla.";
            case "SCENARIO" -> "Yukarıdaki hikâyeyi temel alarak 6-beat YouTube video senaryosu yaz. " +
                    "Hikâyedeki karakter isimlerini, olayları ve duyguları AYNEN kullan. " +
                    "Her beat'in seslendirme metni hikâyenin İLGİLİ BÖLÜMÜNÜ anlatmalı — generic olma!";
            case "STORYBOARD" -> "Yukarıdaki HİKÂYE METNİNİ sahne sahne böl ve her sahne için görsel prompt üret. " +
                    "SESLENDİRME = hikâye metninin kendisi. Yeni cümle UYDURMA, metni BÖLERSIN. " +
                    "Hikâye metnindeki TÜM cümleleri sahnelere dağıt, hiçbir cümle atlanmamalı.";
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
