package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardSourceBeatInput;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-003 Task 09: StoryboardAgentRequest → OpenAI chat messages dönüştürücüsü.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * API key veya secret bilgisi prompt'a dahil edilmez.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Prompt her zaman "draft-only" ve "no auto-approval" talimatı içerir</li>
 *   <li>Medya dosyası (görsel/video/ses) üretim talimatı içermez</li>
 *   <li>Platform yayın paketi talimatı içermez</li>
 * </ul>
 */
class OpenAiStoryboardPromptBuilder {

    private static final String PROMPT_VERSION = "storyboard-v1.0";

    /**
     * System instruction — modelin rolünü ve kurallarını tanımlar.
     */
    static String buildSystemInstruction() {
        return """
                Sen bir Türk kültürü ve tarihi konusunda uzman video storyboard tasarımcısısın.
                Anadolu Atlası platformu için onaylanmış video senaryolarından storyboard üretiyorsun.

                Kurallar:
                - Yanıtını SADECE aşağıda belirtilen JSON formatında ver. Başka metin ekleme.
                - Türkçe yaz (aksi belirtilmedikçe).
                - Senaryo beat'lerini sahne bazlı görsel plana dönüştür.
                - Her sahne için net bir görsel açıklama (visualDescription) yaz — bu zorunludur.
                - Her sahne için kamera açısı, geçiş, atmosfer ve mekan bilgisi ekle.
                - Narration metni, ekran yazıları ve karakter bilgilerini belirle.
                - Tarihî doğruluğa dikkat et.
                - Telif haklı materyalleri kullanma.
                - Kültürel hassasiyetlere saygılı ol.
                - Ürettiğin storyboard her zaman TASLAK niteliğindedir.
                - Storyboard onaylanmış veya yayına hazır değildir.
                - Otomatik onay veya yayın kararı verme.
                - İnsan editör bu storyboard'u inceleyecek ve onaylayacaktır.
                - Görsel, video veya ses dosyası üretme — sadece metin açıklaması yaz.
                - Platform yayın paketi oluşturma.

                Beklenen JSON çıktı formatı:
                {
                  "storyboardTitle": "Storyboard başlığı",
                  "summary": "Kısa özet (1-3 cümle)",
                  "targetPlatform": "YouTube",
                  "aspectRatio": "16:9",
                  "estimatedDurationSeconds": 600,
                  "visualStyleGuide": "Genel görsel stil kılavuzu",
                  "narrationStyle": "Anlatım stili açıklaması",
                  "scenes": [
                    {
                      "sceneNumber": 1,
                      "sourceBeatType": "HOOK",
                      "sourceBeatOrder": 0,
                      "sceneTitle": "Sahne başlığı",
                      "visualDescription": "Sahnede ne gösterilecek — detaylı görsel açıklama (ZORUNLU)",
                      "narrationText": "Voiceover / anlatım metni",
                      "onScreenText": "Ekranda görünecek yazı (varsa)",
                      "cameraDirection": "Kamera hareketi / açısı",
                      "transitionNote": "Sahne geçiş efekti",
                      "mood": "Sahne atmosferi",
                      "location": "Mekan",
                      "characters": "Sahnedeki karakterler",
                      "estimatedDurationSeconds": 10
                    }
                  ]
                }
                """;
    }

    /**
     * StoryboardAgentRequest'ten kullanıcı mesajını oluşturur.
     */
    static String buildUserMessage(StoryboardAgentRequest request) {
        StringBuilder sb = new StringBuilder();

        // ── Scenario Context ──
        sb.append("## Kaynak Senaryo\n");
        appendIfPresent(sb, "Başlık", request.getScenarioTitle());
        appendIfPresent(sb, "Özet", request.getScenarioSummary());
        if (request.getSourceScenarioVersion() != null) {
            sb.append("Senaryo Version: ").append(request.getSourceScenarioVersion()).append("\n");
        }
        sb.append("\n");

        // ── Scenario Beats ──
        if (request.getScenarioBeats() != null && !request.getScenarioBeats().isEmpty()) {
            sb.append("## Senaryo Beat'leri\n");
            for (StoryboardSourceBeatInput beat : request.getScenarioBeats()) {
                sb.append("### ").append(beat.getOrderIndex() + 1).append(". ");
                sb.append(beat.getBeatType() != null ? beat.getBeatType().name() : "UNKNOWN");
                if (beat.getBeatTitle() != null) sb.append(" — ").append(beat.getBeatTitle());
                sb.append("\n");
                if (beat.getBeatBody() != null && !beat.getBeatBody().isBlank()) {
                    sb.append(beat.getBeatBody()).append("\n");
                }
                if (beat.getEstimatedDurationSeconds() != null) {
                    sb.append("Süre: ").append(beat.getEstimatedDurationSeconds()).append(" sn\n");
                }
                sb.append("\n");
            }
        }

        // ── Knowledge Item ──
        if (request.getKnowledgeItemTitle() != null) {
            sb.append("## Bilgi Öğesi\n");
            appendIfPresent(sb, "Başlık", request.getKnowledgeItemTitle());
            appendIfPresent(sb, "Açıklama", request.getKnowledgeItemDescription());
            appendIfPresent(sb, "Şehir", request.getKnowledgeItemCity());
            sb.append("\n");
        }

        // ── Production Instructions ──
        sb.append("## Storyboard Talimatları\n");
        appendIfPresent(sb, "Hedef Platform", request.getTargetPlatform());
        appendIfPresent(sb, "Görüntü Oranı", request.getAspectRatio());
        appendIfPresent(sb, "Hedef Kitle", request.getTargetAudience());
        appendIfPresent(sb, "Ton", request.getTone());
        appendIfPresent(sb, "Dil", request.getLanguage());
        if (request.getEstimatedDurationSeconds() != null) {
            sb.append("Hedef Süre: ").append(request.getEstimatedDurationSeconds()).append(" saniye\n");
        }
        if (request.getDesiredSceneCount() != null) {
            sb.append("İstenen Sahne Sayısı: ").append(request.getDesiredSceneCount()).append("\n");
        }
        appendIfPresent(sb, "Kültürel Bağlam", request.getCulturalContext());
        sb.append("\n");

        // ── Regeneration Context ──
        if (request.getExistingStoryboardVersion() != null && request.getExistingStoryboardVersion() > 0) {
            sb.append("## Yeniden Üretim\n");
            sb.append("Mevcut Version: ").append(request.getExistingStoryboardVersion()).append("\n");
            appendIfPresent(sb, "Neden", request.getGenerationReason());
            appendIfPresent(sb, "Talimat", request.getRegenerationInstruction());
            sb.append("Lütfen farklı bir görsel yaklaşım dene.\n\n");
        }

        // ── Output Reminder ──
        sb.append("## Önemli\n");
        sb.append("Yanıtını SADECE yukarıda belirtilen JSON formatında ver.\n");
        sb.append("Her sahne için visualDescription ZORUNLUDUR.\n");
        sb.append("Sahne numaraları 1'den başlayarak sıralı olmalıdır.\n");
        sb.append("Bu storyboard TASLAK olarak üretilecektir. Otomatik onay veya yayın kararı verme.\n");
        sb.append("Görsel, video veya ses dosyası üretme — sadece metin açıklaması yaz.\n");

        return sb.toString();
    }

    /**
     * Prompt builder tarafından kullanılan mesaj listesini oluşturur.
     */
    static List<OpenAiChatMessage> buildMessages(StoryboardAgentRequest request) {
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
