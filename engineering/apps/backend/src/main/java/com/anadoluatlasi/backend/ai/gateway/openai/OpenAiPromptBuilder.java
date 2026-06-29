package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ContentAgentKnowledgeItemPayload;
import com.anadoluatlasi.backend.ai.contract.HumanFeedbackPayload;
import com.anadoluatlasi.backend.ai.contract.PreviousDraftPayload;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRetryMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-001 Task 23: ContentAgentRequest → OpenAI chat messages dönüştürücüsü.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * API key veya secret bilgisi prompt'a dahil edilmez.</p>
 *
 * <p>Güvenlik kuralı: Prompt her zaman "draft-only" ve "no auto-approval"
 * talimatı içerir.</p>
 */
class OpenAiPromptBuilder {

    private static final String PROMPT_VERSION = "v1.0";

    /**
     * System instruction — modelin rolünü ve kurallarını tanımlar.
     */
    static String buildSystemInstruction() {
        return """
                Sen bir Türk kültürü ve tarihi konusunda uzman içerik editörüsün.
                Anadolu Atlası platformu için özgün, doğru ve etkileyici içerik üretiyorsun.

                Kurallar:
                - Yanıtını SADECE aşağıda belirtilen JSON formatında ver. Başka metin ekleme.
                - Türkçe yaz (aksi belirtilmedikçe).
                - Tarihî doğruluğa dikkat et; emin olmadığın bilgileri "warnings" listesine ekle.
                - Telif haklı materyalleri kullanma.
                - Kültürel hassasiyetlere saygılı ol.
                - Ürettiğin içerik her zaman TASLAK niteliğindedir.
                - İçerik onaylanmış veya yayına hazır değildir.
                - Otomatik onay veya yayın kararı verme.
                - İnsan editör bu taslağı inceleyecek ve onaylayacaktır.

                Beklenen JSON çıktı formatı:
                {
                  "title": "İçerik başlığı",
                  "summary": "Kısa özet (1-3 cümle)",
                  "body": "Ana içerik metni",
                  "suggestedTags": ["etiket1", "etiket2"],
                  "warnings": [
                    { "code": "UYARI_KODU", "message": "Açıklama" }
                  ],
                  "qualityNotes": "AI kalite değerlendirmesi",
                  "generationReason": "Üretim yaklaşımı açıklaması"
                }
                """;
    }

    /**
     * ContentAgentRequest'ten kullanıcı mesajını oluşturur.
     */
    static String buildUserMessage(ContentAgentRequest request) {
        StringBuilder sb = new StringBuilder();

        // ── Knowledge Item ──
        ContentAgentKnowledgeItemPayload ki = request.getKnowledgeItem();
        if (ki != null) {
            sb.append("## Bilgi Öğesi\n");
            appendIfPresent(sb, "Başlık", ki.getTitle());
            appendIfPresent(sb, "Kategori", ki.getCategory());
            appendIfPresent(sb, "Lokasyon", ki.getLocation());
            appendIfPresent(sb, "Açıklama", ki.getDescription());
            appendIfPresent(sb, "Notlar", ki.getNotes());
            sb.append("\n");
        }

        // ── Üretim Talimatları ──
        sb.append("## Üretim Talimatları\n");
        appendIfPresent(sb, "Amaç", request.getContentPurpose());
        appendIfPresent(sb, "Hedef Kitle", request.getTargetAudience());
        appendIfPresent(sb, "Ton", request.getTone());
        appendIfPresent(sb, "Dil", request.getLanguage());
        appendIfPresent(sb, "Format", request.getFormatHint());
        appendIfPresent(sb, "Uzunluk", request.getLengthHint());
        sb.append("\n");

        // ── Human Feedback ──
        HumanFeedbackPayload feedback = request.getHumanFeedback();
        if (feedback != null) {
            sb.append("## İnsan Geri Bildirimi\n");
            appendIfPresent(sb, "Tip", feedback.getFeedbackType());
            appendIfPresent(sb, "Not", feedback.getNote());
            sb.append("\n");
        }

        // ── Previous Draft ──
        PreviousDraftPayload prev = request.getPreviousDraft();
        if (prev != null) {
            sb.append("## Önceki Taslak\n");
            appendIfPresent(sb, "Başlık", prev.getTitle());
            appendIfPresent(sb, "Özet", prev.getSummary());
            appendIfPresent(sb, "İçerik", prev.getBody());
            sb.append("\n");
        }

        // ── Retry Metadata ──
        ContentAgentRetryMetadata retry = request.getRetryMetadata();
        if (retry != null) {
            sb.append("## Retry Bilgisi\n");
            sb.append("Deneme: ").append(retry.getAttemptNumber())
              .append("/").append(retry.getMaxAttempts()).append("\n");
            appendIfPresent(sb, "Önceki hata", retry.getPreviousErrorType());
            sb.append("Lütfen farklı bir yaklaşım dene.\n\n");
        }

        // ── Safety Instructions ──
        if (request.getSafetyInstructions() != null && !request.getSafetyInstructions().isBlank()) {
            sb.append("## Güvenlik\n");
            sb.append(request.getSafetyInstructions()).append("\n\n");
        }

        // ── Output Reminder ──
        sb.append("## Önemli\n");
        sb.append("Yanıtını SADECE yukarıda belirtilen JSON formatında ver. ");
        sb.append("Bu içerik TASLAK olarak üretilecektir. Otomatik onay veya yayın kararı verme.\n");

        return sb.toString();
    }

    /**
     * Prompt builder tarafından kullanılan mesaj listesini oluşturur.
     */
    static List<OpenAiChatMessage> buildMessages(ContentAgentRequest request) {
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
