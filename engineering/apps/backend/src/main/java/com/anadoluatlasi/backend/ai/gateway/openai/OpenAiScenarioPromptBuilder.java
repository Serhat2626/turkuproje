package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-002 Task 09: ScenarioAgentRequest → OpenAI chat messages dönüştürücüsü.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * API key veya secret bilgisi prompt'a dahil edilmez.</p>
 *
 * <p>Güvenlik kuralı: Prompt her zaman "draft-only" ve "no auto-approval"
 * talimatı içerir.</p>
 */
class OpenAiScenarioPromptBuilder {

    private static final String PROMPT_VERSION = "scenario-v1.0";

    /**
     * System instruction — modelin rolünü ve kurallarını tanımlar.
     */
    static String buildSystemInstruction() {
        return """
                Sen bir Türk kültürü ve tarihi konusunda uzman video senaryo yazarısın.
                Anadolu Atlası platformu için YouTube video senaryoları üretiyorsun.

                Kurallar:
                - Yanıtını SADECE aşağıda belirtilen JSON formatında ver. Başka metin ekleme.
                - Türkçe yaz (aksi belirtilmedikçe).
                - Tarihî doğruluğa dikkat et.
                - Telif haklı materyalleri kullanma.
                - Kültürel hassasiyetlere saygılı ol.
                - Ürettiğin senaryo her zaman TASLAK niteliğindedir.
                - Senaryo onaylanmış veya yayına hazır değildir.
                - Otomatik onay veya yayın kararı verme.
                - İnsan editör bu senaryoyu inceleyecek ve onaylayacaktır.

                Senaryo 6 beat'ten oluşmalıdır:
                1. HOOK — İlk 5-10 saniye, izleyiciyi yakala
                2. INTRODUCTION — Konunun tanıtımı
                3. MAIN_NARRATIVE — Ana anlatım, videonun %60'ı
                4. EMOTIONAL_CLIMAX — En duygusal/etkili an
                5. CONCLUSION — Sonuç, tarihî önem
                6. CALL_TO_ACTION — Abone ol, yorum yap, paylaş

                Beklenen JSON çıktı formatı:
                {
                  "scenarioTitle": "Senaryo başlığı",
                  "summary": "Kısa özet (1-3 cümle)",
                  "targetAudience": "Hedef kitle açıklaması",
                  "culturalObjective": "Kültürel/öğretici amaç",
                  "tone": "Anlatım tonu",
                  "estimatedDurationSeconds": 600,
                  "setting": "Mekan/lokasyon",
                  "characters": "Karakterler/anlatıcı",
                  "beats": [
                    {
                      "type": "HOOK",
                      "title": "Beat başlığı",
                      "body": "Beat metni — detaylı anlatım",
                      "order": 1,
                      "estimatedDurationSeconds": 10
                    }
                  ]
                }
                """;
    }

    /**
     * ScenarioAgentRequest'ten kullanıcı mesajını oluşturur.
     */
    static String buildUserMessage(ScenarioAgentRequest request) {
        StringBuilder sb = new StringBuilder();

        // ── Source Draft Context ──
        sb.append("## Kaynak İçerik\n");
        appendIfPresent(sb, "Başlık", request.getSourceDraftTitle());
        appendIfPresent(sb, "Özet", request.getSourceDraftSummary());
        if (request.getSourceDraftBody() != null && !request.getSourceDraftBody().isBlank()) {
            sb.append("İçerik:\n").append(request.getSourceDraftBody()).append("\n\n");
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
        sb.append("## Senaryo Talimatları\n");
        appendIfPresent(sb, "Hedef Kitle", request.getTargetAudience());
        appendIfPresent(sb, "Ton", request.getTone());
        appendIfPresent(sb, "Dil", request.getLanguage());
        if (request.getDesiredDurationSeconds() != null) {
            sb.append("Hedef Süre: ").append(request.getDesiredDurationSeconds()).append(" saniye\n");
        }
        appendIfPresent(sb, "Kültürel Bağlam", request.getCulturalContext());
        sb.append("\n");

        // ── Regeneration Context ──
        if (request.getExistingScenarioVersion() != null && request.getExistingScenarioVersion() > 0) {
            sb.append("## Yeniden Üretim\n");
            sb.append("Mevcut Version: ").append(request.getExistingScenarioVersion()).append("\n");
            appendIfPresent(sb, "Neden", request.getGenerationReason());
            appendIfPresent(sb, "Talimat", request.getRegenerationInstruction());
            sb.append("Lütfen farklı bir yaklaşım dene.\n\n");
        }

        // ── Output Reminder ──
        sb.append("## Önemli\n");
        sb.append("Yanıtını SADECE yukarıda belirtilen JSON formatında ver.\n");
        sb.append("Tam 6 beat üret: HOOK, INTRODUCTION, MAIN_NARRATIVE, EMOTIONAL_CLIMAX, CONCLUSION, CALL_TO_ACTION.\n");
        sb.append("Bu senaryo TASLAK olarak üretilecektir. Otomatik onay veya yayın kararı verme.\n");

        return sb.toString();
    }

    /**
     * Prompt builder tarafından kullanılan mesaj listesini oluşturur.
     */
    static List<OpenAiChatMessage> buildMessages(ScenarioAgentRequest request) {
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
