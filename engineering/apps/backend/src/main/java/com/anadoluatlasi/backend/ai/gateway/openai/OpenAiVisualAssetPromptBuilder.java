package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.VisualAssetPlanAgentRequest;
import com.anadoluatlasi.backend.ai.contract.VisualAssetSceneInput;
import com.anadoluatlasi.backend.ai.contract.VisualAssetThumbnailInput;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-006 Task 09: VisualAssetPlanAgentRequest → OpenAI chat messages dönüştürücüsü.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * API key veya secret bilgisi prompt'a dahil edilmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Prompt her zaman "draft-only" ve "no auto-approval" talimatı içerir</li>
 *   <li>Image generation API çağrısı yapmaz — yalnızca prompt planı üretir</li>
 *   <li>Image URL, dosya referansı, MediaAsset/GeneratedAsset oluşturma talimatı içermez</li>
 *   <li>DALL-E, Midjourney, Stable Diffusion çağrısı yapmaz</li>
 *   <li>READY_FOR_PUBLISH veya publish talimatı içermez</li>
 *   <li>API key, secret, OAuth bilgisi dahil edilmez</li>
 * </ul>
 */
class OpenAiVisualAssetPromptBuilder {

    private static final String PROMPT_VERSION = "visual-asset-plan-v1.0";

    /**
     * System instruction — modelin rolünü ve kurallarını tanımlar.
     */
    static String buildSystemInstruction() {
        return """
                Sen bir Türk kültürü, tarihi ve görsel sanat konusunda uzman dijital içerik görsel planlama uzmanısın.
                Anadolu Atlası platformu için onaylanmış video storyboard'larından sahne bazlı görsel üretim planları oluşturuyorsun.

                Görevin:
                - Her storyboard sahnesi için image-generation-ready İngilizce prompt üret.
                - Her sahne için Türkçe görsel bağlam açıklaması (editor context) üret.
                - Her sahne için negative prompt, stil, aspect ratio, kompozisyon, kamera, aydınlatma, renk paleti, atmosfer, mekan, karakter, kültürel detay, metin overlay ve güvenlik notları üret.
                - Genel görsel stil rehberi, renk paleti rehberi, karakter ve mekan sürekliliği rehberi üret.
                - Thumbnail için image-generation-ready prompt üret.

                Kurallar:
                - Yanıtını SADECE aşağıda belirtilen JSON formatında ver. Başka metin ekleme.
                - İngilizce image prompt'ları detaylı, image-generation-ready, sinematik ve yüksek kaliteli olmalı.
                - Türkçe bağlam açıklamaları editörün anlayacağı doğal Türkçe ile yazılmalı.
                - Anadolu kültürü, gelenekleri ve tarihsel bağlam saygılı şekilde korunmalı.
                - Anakronistik nesneler (modern markalar, logolar) kaynak hikâye gerektirmediği sürece kullanılmamalı.
                - Savaş, göç, yoksulluk, yas gibi hassas konular saygılı ve duyarlı şekilde işlenmeli.
                - Grafik şiddet, çıplaklık veya uygunsuz içerik üretme.
                - Input sahne sırasını koru — çıktıda aynı sıra ve aynı sahne sayısı olmalı.
                - Her sahne için sourceStoryboardSceneId ve sceneNumber koru.
                - Bir thumbnail planı üret.
                - Image URL, dosya yolu, bucket, objectKey gibi depolama referansları üretme.
                - MediaAsset, GeneratedAsset ID'si veya referansı üretme.
                - Görsel dosya üretildiğini iddia etme — sadece prompt planı üretiyorsun.
                - Status ekleme — backend status yönetir.
                - Otomatik onay veya yayın kararı verme.
                - API key, secret, OAuth bilgisi dahil etme.

                Beklenen JSON çıktı formatı:
                {
                  "planTitle": "Görsel plan başlığı",
                  "overallVisualStyle": "Genel stil rehberi",
                  "aspectRatioStrategy": "16:9 primary",
                  "culturalAuthenticityGuidance": "Kültürel doğruluk rehberi",
                  "characterContinuityGuidance": "Karakter sürekliliği",
                  "locationContinuityGuidance": "Mekan sürekliliği",
                  "colorPaletteGuidance": "Renk paleti rehberi",
                  "scenePlans": [
                    {
                      "sourceStoryboardSceneId": "uuid",
                      "sceneNumber": 1,
                      "sourceBeatType": "HOOK",
                      "sceneTitle": "Sahne başlığı",
                      "englishImagePrompt": "Detailed English image-generation-ready prompt...",
                      "turkishVisualContext": "Türkçe görsel bağlam açıklaması...",
                      "negativePrompt": "no text, no watermark, no modern elements",
                      "styleDirective": "Cinematic, documentary",
                      "aspectRatio": "16:9",
                      "compositionNotes": "Rule of thirds, ...",
                      "cameraFraming": "Wide establishing shot",
                      "lightingNotes": "Golden hour, warm tones",
                      "colorPalette": "Earth tones: ochre, sienna, olive",
                      "mood": "Nostalgic, dramatic",
                      "location": "Anadolu köyü, taş evler",
                      "characters": "Yaşlı hikâyeci",
                      "culturalDetails": "Geleneksel kıyafetler, ...",
                      "textOverlayGuidance": "Alt yazı alanı bırak",
                      "imageGenerationToolNotes": "DALL-E/Midjourney uyumlu",
                      "safetyNotes": "Hassas konu: saygılı işle"
                    }
                  ],
                  "thumbnailPlan": {
                    "thumbnailPrompt": "Portrait of storyteller...",
                    "negativePrompt": "no text, no watermark",
                    "textOverlayGuidance": "Başlık için alan bırak",
                    "compositionNotes": "Center subject, ...",
                    "emotionalHook": "Göz teması, duygu",
                    "aspectRatio": "16:9",
                    "styleDirective": "Cinematic portrait",
                    "colorPalette": "Warm, saturated",
                    "sourceSceneNumber": 1,
                    "sourceStoryboardSceneId": "uuid"
                  }
                }
                """;
    }

    /**
     * User mesajı — proje ve sahne context'ini içerir.
     */
    static String buildUserMessage(VisualAssetPlanAgentRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Proje Bilgisi\n");
        sb.append("Başlık: ").append(safe(request.getTitle())).append("\n");
        sb.append("Özet: ").append(safe(request.getSummary())).append("\n");
        sb.append("Hedef platform: ").append(safe(request.getTargetPlatform())).append("\n");
        sb.append("Aspect ratio: ").append(safe(request.getAspectRatio())).append("\n");
        if (request.getEstimatedDurationSeconds() != null) {
            sb.append("Tahmini süre: ").append(request.getEstimatedDurationSeconds()).append(" saniye\n");
        }
        sb.append("Görsel stil rehberi: ").append(safe(request.getVisualStyleGuide())).append("\n");
        sb.append("Anlatım stili: ").append(safe(request.getNarrationStyle())).append("\n");

        if (request.getLanguagePolicy() != null) {
            sb.append("\n## Dil Politikası\n").append(request.getLanguagePolicy()).append("\n");
        }
        if (request.getCulturalContextPolicy() != null) {
            sb.append("\n## Kültürel Bağlam Politikası\n").append(request.getCulturalContextPolicy()).append("\n");
        }
        if (request.getSafetyPolicy() != null) {
            sb.append("\n## İçerik Güvenliği Politikası\n").append(request.getSafetyPolicy()).append("\n");
        }

        // Sahneler
        sb.append("\n## Storyboard Sahneleri (toplam: ").append(
                request.getScenes() != null ? request.getScenes().size() : 0).append(")\n\n");

        if (request.getScenes() != null) {
            for (VisualAssetSceneInput scene : request.getScenes()) {
                sb.append("### Sahne ").append(scene.getSceneNumber()).append("\n");
                sb.append("- sourceStoryboardSceneId: ").append(scene.getStoryboardSceneId()).append("\n");
                sb.append("- Beat tipi: ").append(safe(scene.getSourceBeatType())).append("\n");
                sb.append("- Başlık: ").append(safe(scene.getSceneTitle())).append("\n");
                sb.append("- Görsel tanım: ").append(safe(scene.getVisualDescription())).append("\n");
                sb.append("- Seslendirme: ").append(safe(scene.getNarrationText())).append("\n");
                if (scene.getOnScreenText() != null) {
                    sb.append("- Ekran metni: ").append(scene.getOnScreenText()).append("\n");
                }
                sb.append("- Kamera yönlendirmesi: ").append(safe(scene.getCameraDirection())).append("\n");
                sb.append("- Atmosfer: ").append(safe(scene.getMood())).append("\n");
                sb.append("- Mekan: ").append(safe(scene.getLocation())).append("\n");
                sb.append("- Karakterler: ").append(safe(scene.getCharacters())).append("\n");
                sb.append("\n");
            }
        }

        // Thumbnail context
        if (request.getThumbnailContext() != null) {
            VisualAssetThumbnailInput thumb = request.getThumbnailContext();
            sb.append("## Thumbnail Context\n");
            sb.append("- Başlık: ").append(safe(thumb.getTitle())).append("\n");
            sb.append("- Hikâye hook: ").append(safe(thumb.getStoryHook())).append("\n");
            sb.append("- Aspect ratio: ").append(safe(thumb.getPreferredAspectRatio())).append("\n");
            sb.append("- Duygusal ton: ").append(safe(thumb.getEmotionalTone())).append("\n");
            sb.append("- Metin overlay politikası: ").append(safe(thumb.getTextOverlayPolicy())).append("\n");
        }

        sb.append("\nHer sahne için image-generation-ready İngilizce prompt ve Türkçe bağlam açıklaması üret.");
        sb.append("\nToplam ").append(request.getScenes() != null ? request.getScenes().size() : 0);
        sb.append(" sahne planı ve 1 thumbnail planı üret.");
        sb.append("\nYanıtını JSON formatında ver.");

        return sb.toString();
    }

    /**
     * Chat mesaj listesi oluşturur.
     */
    static List<OpenAiChatMessage> buildMessages(VisualAssetPlanAgentRequest request) {
        List<OpenAiChatMessage> messages = new ArrayList<>();
        messages.add(new OpenAiChatMessage("system", buildSystemInstruction()));
        messages.add(new OpenAiChatMessage("user", buildUserMessage(request)));
        return messages;
    }

    /**
     * Prompt versiyonunu döndürür.
     */
    static String getPromptVersion() {
        return PROMPT_VERSION;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
