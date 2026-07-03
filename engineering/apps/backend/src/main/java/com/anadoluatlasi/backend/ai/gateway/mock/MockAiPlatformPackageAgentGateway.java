package com.anadoluatlasi.backend.ai.gateway.mock;

import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentRequest;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentResponse;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageOutput;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageSceneUsageOutput;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageSourceSceneInput;
import com.anadoluatlasi.backend.ai.gateway.AiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FP-004: Mock AI Platform Package Agent Gateway implementasyonu.
 *
 * <p>Gerçek AI provider'a bağlanmadan platform paketi workflow'unu test edilebilir hale getirir.
 * Database erişimi yapmaz, status güncellemez, draft oluşturmaz.
 * Sadece PlatformPackageAgentRequest → PlatformPackageAgentResponse dönüşümü yapar.</p>
 *
 * <p>Desteklenen senaryolar:</p>
 * <ul>
 *   <li>SUCCESS — deterministic başarılı platform paketleri (her hedef platform için bir paket)</li>
 *   <li>TIMEOUT — provider timeout hatası (retryable)</li>
 *   <li>EMPTY_RESPONSE — boş yanıt hatası (retryable)</li>
 *   <li>INVALID_FORMAT — geçersiz format hatası (retryable)</li>
 *   <li>UNSAFE_CONTENT — güvenli olmayan içerik hatası (non-retryable)</li>
 * </ul>
 *
 * <p>Bu gateway gerçek media dosyası (image/video/audio) üretmez.
 * sceneUsages yalnızca sahne kullanım metadata'sıdır.</p>
 */
@Slf4j
public class MockAiPlatformPackageAgentGateway implements AiPlatformPackageAgentGateway {

    private volatile MockPlatformPackageGatewayMode currentMode;

    public MockAiPlatformPackageAgentGateway() {
        this(MockPlatformPackageGatewayMode.SUCCESS);
    }

    public MockAiPlatformPackageAgentGateway(MockPlatformPackageGatewayMode initialMode) {
        this.currentMode = initialMode;
    }

    /**
     * Aktif modu değiştirir. Test/geliştirme amacıyla kullanılır.
     */
    public void setMode(MockPlatformPackageGatewayMode mode) {
        log.info("Mock Platform Package AI mod değiştirildi: {} → {}", this.currentMode, mode);
        this.currentMode = mode;
    }

    public MockPlatformPackageGatewayMode getMode() {
        return currentMode;
    }

    @Override
    public PlatformPackageAgentResponse generatePlatformPackages(PlatformPackageAgentRequest request) {
        log.info("Mock Platform Package AI çağrısı: requestId={}, mod={}, platforms={}",
                request.getRequestId(), currentMode, request.getTargetPlatforms());

        return switch (currentMode) {
            case SUCCESS -> handleSuccess(request);
            case TIMEOUT -> handleTimeout(request);
            case EMPTY_RESPONSE -> handleEmptyResponse(request);
            case INVALID_FORMAT -> handleInvalidFormat(request);
            case UNSAFE_CONTENT -> handleUnsafeContent(request);
        };
    }

    // ── SUCCESS ────────────────────────────────────────────

    private PlatformPackageAgentResponse handleSuccess(PlatformPackageAgentRequest request) {
        List<PlatformPackageOutput> packages = new ArrayList<>();

        List<PlatformPackageType> targetPlatforms = request.getTargetPlatforms();
        if (targetPlatforms != null) {
            for (PlatformPackageType platformType : targetPlatforms) {
                packages.add(buildPackageForPlatform(platformType, request));
            }
        }

        log.info("Mock Platform Package AI başarılı yanıt: requestId={}, packages={}",
                request.getRequestId(), packages.size());

        return PlatformPackageAgentResponse.builder()
                .success(true)
                .packages(packages)
                .warnings(List.of())
                .providerType("MOCK")
                .modelName("mock-platform-package-v1")
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private PlatformPackageOutput buildPackageForPlatform(
            PlatformPackageType platformType, PlatformPackageAgentRequest request) {

        String subjectTitle = extractSubjectTitle(request);

        return switch (platformType) {
            case YOUTUBE_LONG_FORM -> buildYouTubeLongForm(subjectTitle, request);
            case YOUTUBE_SHORTS -> buildYouTubeShorts(subjectTitle, request);
            case INSTAGRAM_REELS -> buildInstagramReels(subjectTitle, request);
            case FACEBOOK_REELS -> buildFacebookReels(subjectTitle, request);
        };
    }

    /**
     * Storyboard veya knowledgeContext'ten konu başlığını çıkarır.
     */
    private String extractSubjectTitle(PlatformPackageAgentRequest request) {
        if (request.getSourceStoryboard() != null && request.getSourceStoryboard().getTitle() != null) {
            return request.getSourceStoryboard().getTitle();
        }
        if (request.getKnowledgeContext() != null && request.getKnowledgeContext().getTitle() != null) {
            return request.getKnowledgeContext().getTitle();
        }
        return "Anadolu Hikâyesi";
    }

    // ── YouTube Long-Form ──────────────────────────────────

    private PlatformPackageOutput buildYouTubeLongForm(String subjectTitle, PlatformPackageAgentRequest request) {
        return PlatformPackageOutput.builder()
                .packageType(PlatformPackageType.YOUTUBE_LONG_FORM.name())
                .title(subjectTitle + " — YouTube Video")
                .captionOrDescription(String.format(
                        "Anadolu'nun derinliklerinden bir hikâye: %s. Bu videoda, unutulmaya yüz tutmuş " +
                        "bir türkünün ardındaki gerçek hikâyeyi keşfediyoruz.", subjectTitle))
                .hashtags(List.of("#AnadoluAtlası", "#Türkü", "#Hikâye", "#Tarih", "#Kültür"))
                .callToAction("Bu hikâyeyi sevdiyseniz kanalımıza abone olmayı ve bildirimleri açmayı unutmayın!")
                .targetDurationSeconds(600)
                .aspectRatio("16:9")
                .contentStructureNotes("Hook → Giriş → Ana Hikâye → Duygusal Zirve → Kapanış → CTA yapısı")
                .sceneUsageNotes("Tüm storyboard sahneleri sıralı olarak kullanılır.")
                // YouTube Long-Form metadata
                .videoTitle(String.format("%s | Anadolu'nun Unutulmaz Hikâyesi", subjectTitle))
                .longDescription(String.format(
                        "%s — Anadolu Atlası\n\n" +
                        "Bu videoda %s konusunu derinlemesine ele alıyoruz.\n\n" +
                        "📌 Bölümler:\n" +
                        "00:00 Giriş\n" +
                        "01:30 Tarihi Arka Plan\n" +
                        "04:00 Ana Hikâye\n" +
                        "07:30 Duygusal Zirve\n" +
                        "09:00 Kapanış\n\n" +
                        "🔔 Abone olmayı unutmayın!\n" +
                        "📱 Sosyal medyada bizi takip edin.\n\n" +
                        "#AnadoluAtlası #Türkü #Tarih", subjectTitle, subjectTitle))
                .chapterOutline(List.of(
                        "00:00 Giriş",
                        "01:30 Tarihi Arka Plan",
                        "04:00 Ana Hikâye",
                        "07:30 Duygusal Zirve",
                        "09:00 Kapanış"))
                .pinnedCommentSuggestion(String.format(
                        "📌 %s hakkında siz ne biliyorsunuz? Yorumlarda paylaşın! " +
                        "Hangi Anadolu hikâyesini anlatalım? 👇", subjectTitle))
                .thumbnailTextSuggestion(subjectTitle.length() > 30
                        ? subjectTitle.substring(0, 30) + "..."
                        : subjectTitle)
                .tags(List.of("anadolu atlası", "türkü hikâyesi", subjectTitle.toLowerCase(),
                        "tarih", "kültür", "anadolu", "hikâye", "belgesel"))
                .sceneUsages(buildSceneUsages(request, "PRIMARY"))
                .build();
    }

    // ── YouTube Shorts ─────────────────────────────────────

    private PlatformPackageOutput buildYouTubeShorts(String subjectTitle, PlatformPackageAgentRequest request) {
        return buildShortFormPackage(
                PlatformPackageType.YOUTUBE_SHORTS,
                subjectTitle,
                request,
                45,
                "YouTube Shorts",
                "15-45 saniye arası, dikey format. İlk 3 saniyede dikkat çekilmeli.");
    }

    // ── Instagram Reels ────────────────────────────────────

    private PlatformPackageOutput buildInstagramReels(String subjectTitle, PlatformPackageAgentRequest request) {
        return buildShortFormPackage(
                PlatformPackageType.INSTAGRAM_REELS,
                subjectTitle,
                request,
                60,
                "Instagram Reels",
                "30-60 saniye, dikey format. Görsel ağırlıklı, metin overlay ile destekli.");
    }

    // ── Facebook Reels ─────────────────────────────────────

    private PlatformPackageOutput buildFacebookReels(String subjectTitle, PlatformPackageAgentRequest request) {
        return buildShortFormPackage(
                PlatformPackageType.FACEBOOK_REELS,
                subjectTitle,
                request,
                60,
                "Facebook Reels",
                "30-60 saniye, dikey format. Paylaşılabilir, duygusal içerik öne çıkarılmalı.");
    }

    // ── Short-Form ortak builder ───────────────────────────

    private PlatformPackageOutput buildShortFormPackage(
            PlatformPackageType platformType,
            String subjectTitle,
            PlatformPackageAgentRequest request,
            int targetDurationSeconds,
            String platformLabel,
            String durationGuidance) {

        return PlatformPackageOutput.builder()
                .packageType(platformType.name())
                .title(subjectTitle + " — " + platformLabel)
                .captionOrDescription(String.format(
                        "%s hakkında bilmediğiniz gerçek... 🎵 #AnadoluAtlası", subjectTitle))
                .hashtags(List.of("#AnadoluAtlası", "#Türkü", "#Shorts", "#Tarih"))
                .callToAction("Daha fazlası için takip edin! 🔔")
                .targetDurationSeconds(targetDurationSeconds)
                .aspectRatio("9:16")
                .contentStructureNotes(String.format(
                        "Kısa format: Hook (3s) → Öz Bilgi → Duygusal Kapanış. %s için optimize edildi.",
                        platformLabel))
                .sceneUsageNotes("Storyboard sahnelerinden en etkileyici 2-3 tanesi seçilir ve adapte edilir.")
                // Short-Form metadata
                .shortHook(String.format("Bu türkünün arkasındaki hikâyeyi biliyor musunuz? %s...", subjectTitle))
                .shortCaption(String.format(
                        "%s — Anadolu'dan unutulmaz bir hikâye. 🎵\n\n" +
                        "Tam video için linke tıklayın! 👆", subjectTitle))
                .overlayTextSuggestions(List.of(
                        "Bunu biliyor muydunuz? 🤔",
                        subjectTitle,
                        "Hikâyenin devamı için kaydırın →"))
                .shortHashtagSet(List.of(
                        "#AnadoluAtlası", "#Türkü", "#Hikâye",
                        "#" + platformLabel.replace(" ", ""),
                        "#AnadoluHikâyeleri"))
                .shortCta("Tam hikâye için kanalımıza göz atın! 👆")
                .durationGuidance(durationGuidance)
                .sceneUsages(buildSceneUsages(request, "ADAPTED"))
                .build();
    }

    // ── Scene Usage builder ────────────────────────────────

    /**
     * Kaynak sahnelerden scene usage listesi oluşturur.
     * Long-form için PRIMARY, short-form için ADAPTED kullanım tipi atanır.
     */
    private List<PlatformPackageSceneUsageOutput> buildSceneUsages(
            PlatformPackageAgentRequest request, String defaultUsageType) {

        List<PlatformPackageSceneUsageOutput> usages = new ArrayList<>();
        List<PlatformPackageSourceSceneInput> sourceScenes = request.getSourceScenes();

        if (sourceScenes == null || sourceScenes.isEmpty()) {
            return usages;
        }

        for (PlatformPackageSourceSceneInput scene : sourceScenes) {
            boolean isPrimary = "PRIMARY".equals(defaultUsageType);
            String usageType;
            boolean included;
            String adaptationNote;

            if (isPrimary) {
                // Long-form: tüm sahneler dahil
                usageType = "PRIMARY";
                included = true;
                adaptationNote = String.format(
                        "Sahne %d orijinal haliyle kullanılır: %s",
                        scene.getSceneNumber(),
                        scene.getSceneTitle() != null ? scene.getSceneTitle() : "Başlıksız sahne");
            } else {
                // Short-form: ilk 3 sahne dahil, diğerleri omitted
                if (scene.getSceneNumber() <= 3) {
                    usageType = "ADAPTED";
                    included = true;
                    adaptationNote = String.format(
                            "Sahne %d kısa format için adapte edildi: özet narration ve hızlı geçiş",
                            scene.getSceneNumber());
                } else {
                    usageType = "OMITTED";
                    included = false;
                    adaptationNote = String.format(
                            "Sahne %d kısa format için atlandı: süre kısıtlaması",
                            scene.getSceneNumber());
                }
            }

            usages.add(PlatformPackageSceneUsageOutput.builder()
                    .sceneNumber(scene.getSceneNumber())
                    .usageType(usageType)
                    .included(included)
                    .adaptationNote(adaptationNote)
                    .build());
        }

        return usages;
    }

    // ── TIMEOUT ────────────────────────────────────────────

    private PlatformPackageAgentResponse handleTimeout(PlatformPackageAgentRequest request) {
        log.warn("Mock Platform Package AI timeout simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("AI_PROVIDER_TIMEOUT",
                "Platform paketi üretimi zaman aşımına uğradı. (Mock simülasyon)", true);
    }

    // ── EMPTY_RESPONSE ─────────────────────────────────────

    private PlatformPackageAgentResponse handleEmptyResponse(PlatformPackageAgentRequest request) {
        log.warn("Mock Platform Package AI boş yanıt simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("EMPTY_AI_RESPONSE",
                "Platform paketi üretilemedi — boş yanıt. (Mock simülasyon)", true);
    }

    // ── INVALID_FORMAT ─────────────────────────────────────

    private PlatformPackageAgentResponse handleInvalidFormat(PlatformPackageAgentRequest request) {
        log.warn("Mock Platform Package AI geçersiz format simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("INVALID_AI_RESPONSE_FORMAT",
                "Platform paketi geçersiz formatta üretildi. (Mock simülasyon)", true);
    }

    // ── UNSAFE_CONTENT ─────────────────────────────────────

    private PlatformPackageAgentResponse handleUnsafeContent(PlatformPackageAgentRequest request) {
        log.warn("Mock Platform Package AI güvenli olmayan içerik simülasyonu: requestId={}",
                request.getRequestId());
        return buildFailureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                "Platform paketi güvenlik kontrolünden geçemedi. (Mock simülasyon)", false);
    }

    // ── Failure Helper ─────────────────────────────────────

    private PlatformPackageAgentResponse buildFailureResponse(
            String errorCode, String errorMessage, boolean retryable) {
        return PlatformPackageAgentResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(retryable)
                .providerType("MOCK")
                .modelName("mock-platform-package-v1")
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
