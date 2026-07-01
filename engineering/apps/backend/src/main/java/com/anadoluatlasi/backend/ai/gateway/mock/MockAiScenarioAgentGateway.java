package com.anadoluatlasi.backend.ai.gateway.mock;

import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ScenarioBeatResult;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-002: Mock AI Scenario Agent Gateway implementasyonu.
 *
 * <p>Gerçek AI provider'a bağlanmadan scenario workflow'unu test edilebilir hale getirir.
 * Database erişimi yapmaz, status güncellemez, draft oluşturmaz.
 * Sadece ScenarioAgentRequest → ScenarioAgentResponse dönüşümü yapar.</p>
 *
 * <p>Desteklenen senaryolar:</p>
 * <ul>
 *   <li>SUCCESS — deterministic başarılı 6-beat yanıt</li>
 *   <li>TIMEOUT — provider timeout hatası (retryable)</li>
 *   <li>EMPTY_RESPONSE — boş yanıt hatası (retryable)</li>
 *   <li>INVALID_FORMAT — geçersiz format hatası (retryable)</li>
 *   <li>UNSAFE_CONTENT — güvenli olmayan içerik hatası (non-retryable)</li>
 * </ul>
 */
@Slf4j
public class MockAiScenarioAgentGateway implements AiScenarioAgentGateway {

    private volatile MockScenarioGatewayMode currentMode;

    public MockAiScenarioAgentGateway() {
        this(MockScenarioGatewayMode.SUCCESS);
    }

    public MockAiScenarioAgentGateway(MockScenarioGatewayMode initialMode) {
        this.currentMode = initialMode;
    }

    /**
     * Aktif modu değiştirir. Test/geliştirme amacıyla kullanılır.
     */
    public void setMode(MockScenarioGatewayMode mode) {
        log.info("Mock Scenario AI mod değiştirildi: {} → {}", this.currentMode, mode);
        this.currentMode = mode;
    }

    public MockScenarioGatewayMode getMode() {
        return currentMode;
    }

    @Override
    public ScenarioAgentResponse generateScenario(ScenarioAgentRequest request) {
        log.info("Mock Scenario AI çağrısı: requestId={}, mod={}", request.getRequestId(), currentMode);

        return switch (currentMode) {
            case SUCCESS -> handleSuccess(request);
            case TIMEOUT -> handleTimeout(request);
            case EMPTY_RESPONSE -> handleEmptyResponse(request);
            case INVALID_FORMAT -> handleInvalidFormat(request);
            case UNSAFE_CONTENT -> handleUnsafeContent(request);
        };
    }

    // ── SUCCESS ────────────────────────────────────────────

    private ScenarioAgentResponse handleSuccess(ScenarioAgentRequest request) {
        String subjectTitle = request.getSourceDraftTitle() != null
                ? request.getSourceDraftTitle()
                : "Bilinmeyen Konu";

        String scenarioTitle = subjectTitle + " — Senaryo";

        List<ScenarioBeatResult> beats = List.of(
                ScenarioBeatResult.builder()
                        .type(ScenarioBeatType.HOOK)
                        .title(ScenarioBeatType.HOOK.getLabel())
                        .body(String.format("Antik çağın en ilginç hikâyelerinden biri... %s hakkında hiç düşündünüz mü?", subjectTitle))
                        .order(1)
                        .estimatedDurationSeconds(10)
                        .build(),
                ScenarioBeatResult.builder()
                        .type(ScenarioBeatType.INTRODUCTION)
                        .title(ScenarioBeatType.INTRODUCTION.getLabel())
                        .body(String.format("%s, Anadolu'nun zengin kültürel mirasının önemli bir parçasıdır. Bu bölümde konuyu tanıyalım.", subjectTitle))
                        .order(2)
                        .estimatedDurationSeconds(30)
                        .build(),
                ScenarioBeatResult.builder()
                        .type(ScenarioBeatType.MAIN_NARRATIVE)
                        .title(ScenarioBeatType.MAIN_NARRATIVE.getLabel())
                        .body(String.format("%s tarih boyunca birçok farklı dönemden izler taşımaktadır. Bu dönemler boyunca yaşanan değişimler, toplumsal ve kültürel yapıyı derinden etkilemiştir. Detaylı anlatım bloğu burada yer alır.", subjectTitle))
                        .order(3)
                        .estimatedDurationSeconds(180)
                        .build(),
                ScenarioBeatResult.builder()
                        .type(ScenarioBeatType.EMOTIONAL_CLIMAX)
                        .title(ScenarioBeatType.EMOTIONAL_CLIMAX.getLabel())
                        .body(String.format("Bu hikâyenin en etkili anı... %s ile ilgili en duygusal detay burada paylaşılır.", subjectTitle))
                        .order(4)
                        .estimatedDurationSeconds(60)
                        .build(),
                ScenarioBeatResult.builder()
                        .type(ScenarioBeatType.CONCLUSION)
                        .title(ScenarioBeatType.CONCLUSION.getLabel())
                        .body(String.format("%s hakkında öğrendiklerimizi özetleyelim. Tarihî önemi ve bugüne yansımaları.", subjectTitle))
                        .order(5)
                        .estimatedDurationSeconds(45)
                        .build(),
                ScenarioBeatResult.builder()
                        .type(ScenarioBeatType.CALL_TO_ACTION)
                        .title(ScenarioBeatType.CALL_TO_ACTION.getLabel())
                        .body("Bu videoyu beğendiyseniz abone olmayı unutmayın! Yorumlarınızda hangi Anadolu hikâyesini duymak istediğinizi yazın.")
                        .order(6)
                        .estimatedDurationSeconds(15)
                        .build()
        );

        String summary = String.format("%s hakkında yapılandırılmış video senaryosu.", subjectTitle);

        log.info("Mock Scenario AI başarılı yanıt: requestId={}, beats={}",
                request.getRequestId(), beats.size());

        return ScenarioAgentResponse.builder()
                .success(true)
                .scenarioTitle(scenarioTitle)
                .summary(summary)
                .targetAudience(request.getTargetAudience() != null ? request.getTargetAudience() : "Genel izleyici")
                .culturalObjective("Anadolu kültürel mirasını tanıtmak")
                .tone(request.getTone() != null ? request.getTone() : "Hikâye anlatıcı")
                .estimatedDurationSeconds(340)
                .beats(beats)
                .characters("Anlatıcı")
                .setting(request.getKnowledgeItemCity() != null ? request.getKnowledgeItemCity() : "Anadolu")
                .warnings(List.of())
                .qualityNotes("Mock AI tarafından üretildi. Gerçek AI entegrasyonunda bu alan kalite notları içerecek.")
                .providerType("MOCK")
                .modelName("mock-scenario-v1")
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── TIMEOUT ────────────────────────────────────────────

    private ScenarioAgentResponse handleTimeout(ScenarioAgentRequest request) {
        log.warn("Mock Scenario AI timeout simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("AI_PROVIDER_TIMEOUT",
                "Senaryo üretimi zaman aşımına uğradı. (Mock simülasyon)", true);
    }

    // ── EMPTY_RESPONSE ─────────────────────────────────────

    private ScenarioAgentResponse handleEmptyResponse(ScenarioAgentRequest request) {
        log.warn("Mock Scenario AI boş yanıt simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("EMPTY_AI_RESPONSE",
                "Senaryo üretilemedi — boş yanıt. (Mock simülasyon)", true);
    }

    // ── INVALID_FORMAT ─────────────────────────────────────

    private ScenarioAgentResponse handleInvalidFormat(ScenarioAgentRequest request) {
        log.warn("Mock Scenario AI geçersiz format simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("INVALID_AI_RESPONSE_FORMAT",
                "Senaryo geçersiz formatta üretildi. (Mock simülasyon)", true);
    }

    // ── UNSAFE_CONTENT ─────────────────────────────────────

    private ScenarioAgentResponse handleUnsafeContent(ScenarioAgentRequest request) {
        log.warn("Mock Scenario AI güvenli olmayan içerik simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                "Senaryo güvenlik kontrolünden geçemedi. (Mock simülasyon)", false);
    }

    // ── Failure Helper ─────────────────────────────────────

    private ScenarioAgentResponse buildFailureResponse(String errorCode, String errorMessage, boolean retryable) {
        return ScenarioAgentResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(retryable)
                .providerType("MOCK")
                .modelName("mock-scenario-v1")
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
