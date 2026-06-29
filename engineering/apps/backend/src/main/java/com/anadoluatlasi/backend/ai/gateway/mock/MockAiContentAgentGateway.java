package com.anadoluatlasi.backend.ai.gateway.mock;

import com.anadoluatlasi.backend.ai.contract.ContentAgentError;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ProviderExecutionMetadata;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-001: Mock AI Content Agent Gateway implementasyonu.
 *
 * <p>Gerçek AI provider'a bağlanmadan workflow'u test edilebilir hale getirir.
 * Database erişimi yapmaz, status güncellemez, draft oluşturmaz.
 * Sadece ContentAgentRequest → AiGatewayResult dönüşümü yapar.</p>
 *
 * <p>Desteklenen senaryolar:</p>
 * <ul>
 *   <li>SUCCESS — deterministic başarılı yanıt</li>
 *   <li>TIMEOUT — provider timeout hatası (retryable)</li>
 *   <li>EMPTY_RESPONSE — boş yanıt hatası (retryable)</li>
 *   <li>TOO_SHORT_RESPONSE — çok kısa yanıt hatası (retryable)</li>
 *   <li>INVALID_FORMAT — geçersiz format hatası (retryable)</li>
 *   <li>UNSAFE_CONTENT — güvenli olmayan içerik hatası (non-retryable)</li>
 * </ul>
 *
 * <p>Not: Bu sınıf {@code @Service} veya {@code @Component} olarak işaretlenmemiştir.
 * Spring context'e kaydı configuration veya conditional bean olarak yapılmalıdır.</p>
 */
@Slf4j
public class MockAiContentAgentGateway implements AiContentAgentGateway {

    private volatile MockAiScenario currentScenario;

    public MockAiContentAgentGateway() {
        this(MockAiScenario.SUCCESS);
    }

    public MockAiContentAgentGateway(MockAiScenario initialScenario) {
        this.currentScenario = initialScenario;
    }

    /**
     * Aktif senaryoyu değiştirir. Test/geliştirme amacıyla kullanılır.
     */
    public void setScenario(MockAiScenario scenario) {
        log.info("Mock AI senaryo değiştirildi: {} → {}", this.currentScenario, scenario);
        this.currentScenario = scenario;
    }

    /**
     * Aktif senaryoyu döndürür.
     */
    public MockAiScenario getScenario() {
        return currentScenario;
    }

    @Override
    public AiGatewayResult generateContentDraft(ContentAgentRequest request) {
        log.info("Mock AI çağrısı: requestId={}, senaryo={}", request.getRequestId(), currentScenario);

        return switch (currentScenario) {
            case SUCCESS -> handleSuccess(request);
            case TIMEOUT -> handleTimeout(request);
            case EMPTY_RESPONSE -> handleEmptyResponse(request);
            case TOO_SHORT_RESPONSE -> handleTooShortResponse(request);
            case INVALID_FORMAT -> handleInvalidFormat(request);
            case UNSAFE_CONTENT -> handleUnsafeContent(request);
        };
    }

    // ── SUCCESS ─────────────────────────────────────────────

    private AiGatewayResult handleSuccess(ContentAgentRequest request) {
        String kiTitle = request.getKnowledgeItem() != null
                ? request.getKnowledgeItem().getTitle()
                : "Bilinmeyen Konu";

        String purpose = request.getContentPurpose() != null ? request.getContentPurpose() : "INFORMATIONAL_TEXT";
        String audience = request.getTargetAudience() != null ? request.getTargetAudience() : "GENERAL_USER";
        String tone = request.getTone() != null ? request.getTone() : "INFORMATIVE";
        String language = request.getLanguage() != null ? request.getLanguage() : "tr";

        String title = kiTitle + " — " + purposeLabel(purpose);

        String body = buildDeterministicBody(kiTitle, purpose, audience, tone, language);

        String summary = String.format(
                "%s hakkında %s hedef kitleye yönelik %s tonunda hazırlanmış %s içerik.",
                kiTitle, audienceLabel(audience), toneLabel(tone), purposeLabel(purpose));

        ContentAgentResponse response = ContentAgentResponse.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .title(title)
                .body(body)
                .summary(summary)
                .suggestedTags(List.of(kiTitle, purposeLabel(purpose), language))
                .warnings(List.of())
                .qualityNotes("Mock AI tarafından üretildi. Gerçek AI entegrasyonunda bu alan kalite notları içerecek.")
                .generationReason("Deterministic mock response — " + purpose + " / " + audience + " / " + tone)
                .providerMetadata(ProviderExecutionMetadata.builder()
                        .providerName("mock")
                        .modelName("mock-v1")
                        .durationMs(42L)
                        .promptVersion("mock-1.0")
                        .build())
                .build();

        log.info("Mock AI başarılı yanıt: requestId={}, bodyLength={}",
                request.getRequestId(), body.length());

        return AiGatewayResult.success(response);
    }

    // ── TIMEOUT ─────────────────────────────────────────────

    private AiGatewayResult handleTimeout(ContentAgentRequest request) {
        log.warn("Mock AI timeout simülasyonu: requestId={}", request.getRequestId());

        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .errorType(AiErrorType.AI_PROVIDER_TIMEOUT)
                .message("AI provider belirlenen süre içinde cevap vermedi. (Mock simülasyon)")
                .retryable(true)
                .providerErrorCode("MOCK_TIMEOUT")
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ── EMPTY_RESPONSE ──────────────────────────────────────

    private AiGatewayResult handleEmptyResponse(ContentAgentRequest request) {
        log.warn("Mock AI boş yanıt simülasyonu: requestId={}", request.getRequestId());

        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .errorType(AiErrorType.EMPTY_AI_RESPONSE)
                .message("AI provider boş yanıt döndü. (Mock simülasyon)")
                .retryable(true)
                .providerErrorCode("MOCK_EMPTY")
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ── TOO_SHORT_RESPONSE ──────────────────────────────────

    private AiGatewayResult handleTooShortResponse(ContentAgentRequest request) {
        log.warn("Mock AI kısa yanıt simülasyonu: requestId={}", request.getRequestId());

        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .errorType(AiErrorType.AI_RESPONSE_TOO_SHORT)
                .message("AI yanıtı minimum uzunluk gereksinimini karşılamıyor. (Mock simülasyon)")
                .retryable(true)
                .providerErrorCode("MOCK_TOO_SHORT")
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ── INVALID_FORMAT ──────────────────────────────────────

    private AiGatewayResult handleInvalidFormat(ContentAgentRequest request) {
        log.warn("Mock AI geçersiz format simülasyonu: requestId={}", request.getRequestId());

        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .errorType(AiErrorType.INVALID_AI_RESPONSE_FORMAT)
                .message("AI yanıtı beklenen formata uymuyor. (Mock simülasyon)")
                .retryable(true)
                .providerErrorCode("MOCK_INVALID_FORMAT")
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ── UNSAFE_CONTENT ──────────────────────────────────────

    private AiGatewayResult handleUnsafeContent(ContentAgentRequest request) {
        log.warn("Mock AI güvenli olmayan içerik simülasyonu: requestId={}", request.getRequestId());

        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .errorType(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT)
                .message("AI yanıtı güvenlik kontrolünden geçemedi. (Mock simülasyon)")
                .retryable(false)
                .providerErrorCode("MOCK_UNSAFE")
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ── Deterministic Body Builder ──────────────────────────

    private String buildDeterministicBody(String title, String purpose, String audience,
                                          String tone, String language) {
        StringBuilder sb = new StringBuilder();

        sb.append(title).append(" hakkında detaylı bilgi.\n\n");

        sb.append("Bu içerik ").append(purposeLabel(purpose))
                .append(" amacıyla hazırlanmıştır. ");
        sb.append("Hedef kitle: ").append(audienceLabel(audience)).append(". ");
        sb.append("Anlatım tonu: ").append(toneLabel(tone)).append(".\n\n");

        sb.append(title).append(", Anadolu'nun zengin kültürel mirasının önemli bir parçasıdır. ");
        sb.append("Bu konu hakkında bilgi edinmek, kültürel farkındalığımızı artırmak ");
        sb.append("ve gelecek nesillere bu değerleri aktarmak açısından büyük önem taşımaktadır.\n\n");

        sb.append("Tarihî süreç içinde ").append(title);
        sb.append(" birçok farklı dönemden izler taşımaktadır. ");
        sb.append("Bu dönemler boyunca yaşanan değişimler, toplumsal ve kültürel yapıyı ");
        sb.append("derinden etkilemiştir.\n\n");

        sb.append("Günümüzde ").append(title);
        sb.append(" hem yerel hem de ulusal düzeyde ilgi görmektedir. ");
        sb.append("Kültürel miras koruma çalışmaları kapsamında bu konunun ");
        sb.append("belgelenmesi ve tanıtılması sürdürülmektedir.\n\n");

        sb.append("Dil: ").append(language).append("\n");
        sb.append("[Mock AI tarafından üretilmiştir]");

        return sb.toString();
    }

    // ── Label Helpers ───────────────────────────────────────

    private String purposeLabel(String purpose) {
        return switch (purpose) {
            case "INFORMATIONAL_TEXT" -> "bilgilendirici metin";
            case "CHILD_FRIENDLY_TEXT" -> "çocuklara uygun metin";
            case "TOURISTIC_GUIDE_TEXT" -> "turistik rehber metni";
            case "SOCIAL_MEDIA_POST" -> "sosyal medya içeriği";
            case "MOBILE_APP_CARD" -> "mobil uygulama kartı";
            case "AUDIO_GUIDE_SCRIPT" -> "sesli rehber metni";
            default -> purpose;
        };
    }

    private String audienceLabel(String audience) {
        return switch (audience) {
            case "GENERAL_USER" -> "genel kullanıcı";
            case "CHILD" -> "çocuk";
            case "TOURIST" -> "turist";
            case "STUDENT" -> "öğrenci";
            case "RESEARCHER" -> "araştırmacı";
            case "MOBILE_APP_USER" -> "mobil uygulama kullanıcısı";
            default -> audience;
        };
    }

    private String toneLabel(String tone) {
        return switch (tone) {
            case "INFORMATIVE" -> "bilgilendirici";
            case "SIMPLE" -> "sade";
            case "STORYTELLING" -> "hikâye anlatıcı";
            case "NON_ACADEMIC" -> "akademik olmayan";
            case "CHILD_FRIENDLY" -> "çocuklara uygun";
            case "TOURISTIC" -> "turistik";
            case "SHORT_AND_CLEAR" -> "kısa ve net";
            default -> tone;
        };
    }
}
