package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentRequest;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageSourceSceneInput;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-004 Task 09: PlatformPackageAgentRequest → OpenAI chat messages dönüştürücüsü.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * API key veya secret bilgisi prompt'a dahil edilmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Prompt her zaman "draft-only" ve "no auto-approval" talimatı içerir</li>
 *   <li>Medya dosyası (görsel/video/ses) üretim talimatı içermez</li>
 *   <li>READY_FOR_PUBLISH veya publish talimatı içermez</li>
 *   <li>API key, secret, OAuth bilgisi dahil edilmez</li>
 * </ul>
 */
class OpenAiPlatformPackagePromptBuilder {

    private static final String PROMPT_VERSION = "platform-package-v1.0";

    /**
     * System instruction — modelin rolünü ve kurallarını tanımlar.
     */
    static String buildSystemInstruction() {
        return """
                Sen bir Türk kültürü ve tarihi konusunda uzman dijital içerik stratejisti ve platform paketleme uzmanısın.
                Anadolu Atlası platformu için onaylanmış video storyboard'larından platform-spesifik içerik paketleri üretiyorsun.

                Kurallar:
                - Yanıtını SADECE aşağıda belirtilen JSON formatında ver. Başka metin ekleme.
                - Türkçe yaz (aksi belirtilmedikçe).
                - Her hedef platform için TAM BİR paket üret.
                - Paketler birbirinden bağımsız ve platform-spesifik olmalıdır.
                - YouTube Long-Form paketleri SEO-optimize başlık, detaylı açıklama, chapter outline, sabitlenecek yorum ve thumbnail metin içermeli.
                - Shorts/Reels paketleri ilk 3 saniye hook, kısa caption, ekran üzeri metin önerileri, hashtag seti, CTA ve süre rehberi içermeli.
                - Tarihî doğruluğa dikkat et.
                - Telif haklı materyalleri kullanma.
                - Kültürel hassasiyetlere saygılı ol.
                - Ürettiğin paketler her zaman TASLAK (AI_GENERATED) niteliğindedir.
                - Paketlere status ekleme — backend status yönetir.
                - Paketleri onaylama veya yayına hazır olarak işaretleme.
                - Otomatik onay veya yayın kararı verme.
                - İnsan editör bu paketleri inceleyecek ve onaylayacaktır.
                - Görsel, video veya ses dosyası üretme.
                - OAuth, sosyal medya hesap erişimi veya API key bilgisi dahil etme.
                - Medya dosyası referansı veya URL oluşturma.

                Beklenen JSON çıktı formatı:
                {
                  "packages": [
                    {
                      "packageType": "YOUTUBE_LONG_FORM",
                      "title": "İçerik paketi başlığı",
                      "captionOrDescription": "Platform açıklaması",
                      "hashtags": ["#hashtag1", "#hashtag2"],
                      "callToAction": "CTA metni",
                      "targetDurationSeconds": 600,
                      "aspectRatio": "16:9",
                      "contentStructureNotes": "İçerik yapısı notları",
                      "sceneUsageNotes": "Sahne kullanım rehberi",
                      "videoTitle": "SEO-optimize YouTube başlığı",
                      "longDescription": "Detaylı YouTube açıklaması",
                      "chapterOutline": ["00:00 Giriş", "01:00 Konu"],
                      "pinnedCommentSuggestion": "Sabitlenecek yorum",
                      "thumbnailTextSuggestion": "Thumbnail metin önerisi",
                      "tags": ["tag1", "tag2"],
                      "sceneUsages": [
                        {
                          "sceneNumber": 1,
                          "usageType": "PRIMARY",
                          "included": true,
                          "adaptationNote": "Tam kullanım"
                        }
                      ]
                    },
                    {
                      "packageType": "YOUTUBE_SHORTS",
                      "title": "Shorts başlığı",
                      "captionOrDescription": "Shorts açıklaması",
                      "hashtags": ["#shorts", "#hashtag"],
                      "callToAction": "Abone ol!",
                      "targetDurationSeconds": 60,
                      "aspectRatio": "9:16",
                      "contentStructureNotes": "Kısa format yapısı",
                      "sceneUsageNotes": "Sahne kullanım rehberi",
                      "shortHook": "İlk 3 saniye hook metni",
                      "shortCaption": "Kısa caption",
                      "overlayTextSuggestions": ["Overlay 1", "Overlay 2"],
                      "shortHashtagSet": ["#shorts", "#viral"],
                      "shortCta": "Beğen ve abone ol!",
                      "durationGuidance": "30-60 saniye arası",
                      "sceneUsages": [
                        {
                          "sceneNumber": 1,
                          "usageType": "ADAPTED",
                          "included": true,
                          "adaptationNote": "Kısaltılarak kullanım"
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    /**
     * PlatformPackageAgentRequest'ten kullanıcı mesajını oluşturur.
     */
    static String buildUserMessage(PlatformPackageAgentRequest request) {
        StringBuilder sb = new StringBuilder();

        // ── Storyboard Context ──
        if (request.getSourceStoryboard() != null) {
            sb.append("## Kaynak Storyboard\n");
            appendIfPresent(sb, "Başlık", request.getSourceStoryboard().getTitle());
            appendIfPresent(sb, "Özet", request.getSourceStoryboard().getSummary());
            appendIfPresent(sb, "Görsel Stil", request.getSourceStoryboard().getVisualStyleGuide());
            appendIfPresent(sb, "Anlatım Stili", request.getSourceStoryboard().getNarrationStyle());
            appendIfPresent(sb, "Hedef Platform", request.getSourceStoryboard().getTargetPlatform());
            appendIfPresent(sb, "Aspect Ratio", request.getSourceStoryboard().getAspectRatio());
            if (request.getSourceStoryboard().getEstimatedDurationSeconds() != null) {
                sb.append("Tahmini Süre: ").append(request.getSourceStoryboard().getEstimatedDurationSeconds()).append(" saniye\n");
            }
            if (request.getSourceStoryboard().getTotalSceneCount() != null) {
                sb.append("Toplam Sahne: ").append(request.getSourceStoryboard().getTotalSceneCount()).append("\n");
            }
            sb.append("\n");
        }

        // ── Storyboard Scenes ──
        if (request.getSourceScenes() != null && !request.getSourceScenes().isEmpty()) {
            sb.append("## Storyboard Sahneleri\n");
            for (PlatformPackageSourceSceneInput scene : request.getSourceScenes()) {
                sb.append("### Sahne ").append(scene.getSceneNumber());
                if (scene.getSceneTitle() != null) sb.append(" — ").append(scene.getSceneTitle());
                sb.append("\n");
                appendIfPresent(sb, "Görsel", scene.getVisualDescription());
                appendIfPresent(sb, "Narration", scene.getNarrationText());
                appendIfPresent(sb, "Mood", scene.getMood());
                appendIfPresent(sb, "Beat Tipi", scene.getSourceBeatType());
                if (scene.getEstimatedDurationSeconds() > 0) {
                    sb.append("Süre: ").append(scene.getEstimatedDurationSeconds()).append(" sn\n");
                }
                sb.append("\n");
            }
        }

        // ── Knowledge Context ──
        if (request.getKnowledgeContext() != null) {
            sb.append("## Bilgi Bağlamı\n");
            appendIfPresent(sb, "Konu", request.getKnowledgeContext().getTitle());
            appendIfPresent(sb, "Açıklama", request.getKnowledgeContext().getDescription());
            appendIfPresent(sb, "Şehir", request.getKnowledgeContext().getCity());
            appendIfPresent(sb, "Dönem", request.getKnowledgeContext().getPeriod());
            sb.append("\n");
        }

        // ── Target Platforms ──
        sb.append("## Hedef Platformlar\n");
        if (request.getTargetPlatforms() != null) {
            for (PlatformPackageType platform : request.getTargetPlatforms()) {
                sb.append("- ").append(platform.name()).append("\n");
            }
        }
        sb.append("\n");

        // ── Platform-Specific Instructions ──
        sb.append("## Platform Bazlı Çıktı Beklentileri\n\n");
        sb.append("### YouTube Long-Form (YOUTUBE_LONG_FORM)\n");
        sb.append("- videoTitle: SEO-optimize YouTube başlığı (zorunlu)\n");
        sb.append("- longDescription: Detaylı YouTube video açıklaması, linkler ve anahtar kelimeler dahil (zorunlu)\n");
        sb.append("- chapterOutline: YouTube chapters listesi, zaman damgalı (zorunlu)\n");
        sb.append("- pinnedCommentSuggestion: Sabitlenecek yorum önerisi (zorunlu)\n");
        sb.append("- thumbnailTextSuggestion: Thumbnail üzerine yazılacak metin (zorunlu)\n");
        sb.append("- tags: Video etiketleri\n\n");

        sb.append("### YouTube Shorts (YOUTUBE_SHORTS), Instagram Reels (INSTAGRAM_REELS), Facebook Reels (FACEBOOK_REELS)\n");
        sb.append("- shortHook: İlk 3 saniye hook metni (zorunlu)\n");
        sb.append("- shortCaption: Kısa platform açıklaması (zorunlu)\n");
        sb.append("- overlayTextSuggestions: Ekran üzeri metin önerileri (zorunlu)\n");
        sb.append("- shortHashtagSet: Platform-spesifik hashtag seti (zorunlu)\n");
        sb.append("- shortCta: Kısa CTA (zorunlu)\n");
        sb.append("- durationGuidance: Süre rehberi (zorunlu)\n\n");

        // ── Tone/Style/Language ──
        sb.append("## Stil Rehberi\n");
        appendIfPresent(sb, "Dil", request.getLanguage());
        appendIfPresent(sb, "Ton", request.getToneGuidance());
        appendIfPresent(sb, "Hedef Kitle", request.getTargetAudience());
        sb.append("\n");

        // ── Output Reminder ──
        sb.append("## Önemli\n");
        sb.append("Yanıtını SADECE yukarıda belirtilen JSON formatında ver.\n");
        sb.append("Her hedef platform için TAM BİR paket üret. Eksik platform kabul edilmez.\n");
        sb.append("Paketlere status, onay veya yayın durumu EKLEMİYORSUN.\n");
        sb.append("Bu paketler TASLAK olarak üretilecektir. Otomatik onay veya yayın kararı verme.\n");
        sb.append("Görsel, video veya ses dosyası üretme — sadece metin içerik oluştur.\n");
        sb.append("OAuth, API key, secret veya sosyal medya hesap bilgisi dahil etme.\n");

        return sb.toString();
    }

    /**
     * Prompt builder tarafından kullanılan mesaj listesini oluşturur.
     */
    static List<OpenAiChatMessage> buildMessages(PlatformPackageAgentRequest request) {
        List<OpenAiChatMessage> messages = new ArrayList<>();
        messages.add(new OpenAiChatMessage("system", buildSystemInstruction()));
        messages.add(new OpenAiChatMessage("user", buildUserMessage(request)));
        return messages;
    }

    /**
     * Prompt versiyonunu döner.
     */
    static String getPromptVersion() {
        return PROMPT_VERSION;
    }

    private static void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }
}
