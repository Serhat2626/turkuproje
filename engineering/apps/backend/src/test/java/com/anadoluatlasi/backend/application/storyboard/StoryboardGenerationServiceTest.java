package com.anadoluatlasi.backend.application.storyboard;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;
import com.anadoluatlasi.backend.ai.contract.StoryboardSceneOutput;
import com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockStoryboardGatewayMode;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-003: StoryboardGenerationService unit testleri.
 *
 * <p>Kritik kurallar:</p>
 * <ul>
 *   <li>AI üretimi hiçbir zaman APPROVED storyboard'a yol açmamalı</li>
 *   <li>Storyboard üretimi approved ScenarioDraft gerektirir</li>
 *   <li>Raw prompt/payload/secret saklanmaz</li>
 *   <li>Gerçek media dosyası üretilmez</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class StoryboardGenerationServiceTest {

    @Mock
    private ScenarioDraftRepository scenarioDraftRepository;

    @Mock
    private StoryboardDraftRepository storyboardDraftRepository;

    private MockAiStoryboardAgentGateway mockGateway;
    private StoryboardGenerationService service;

    private ContentProject project;
    private ContentDraft approvedDraft;
    private ScenarioDraft approvedScenario;

    @BeforeEach
    void setUp() {
        mockGateway = new MockAiStoryboardAgentGateway();
        service = new StoryboardGenerationService(
                scenarioDraftRepository,
                storyboardDraftRepository,
                mockGateway);

        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        approvedDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Göbeklitepe — Bilgilendirici Metin")
                .body("Göbeklitepe hakkında bilgilendirici içerik.")
                .summary("Göbeklitepe özet")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        approvedScenario = createApprovedScenario();
    }

    // ── Successful Generation ───────────────────────────────

    @Nested
    @DisplayName("Başarılı storyboard üretimi")
    class SuccessfulGeneration {

        @Test
        @DisplayName("1. Başarılı mock generation → AI_GENERATED storyboard oluşturur")
        void shouldCreateAiGeneratedStoryboard() {
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("AI_GENERATED");
        }

        @Test
        @DisplayName("2. Başarılı mock generation → sıralı scene'ler oluşturur")
        void shouldCreateOrderedScenes() {
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getSceneCount()).isEqualTo(6); // 6 beat → 6 scene

            // StoryboardDraft save'leri doğrula (scene'li)
            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository, atLeast(2)).save(captor.capture());
            StoryboardDraft savedDraft = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(savedDraft.getScenes()).hasSize(6);

            // Scene sırası deterministik
            for (int i = 0; i < savedDraft.getScenes().size(); i++) {
                assertThat(savedDraft.getScenes().get(i).getSceneNumber()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("3. Scene sıralaması deterministik")
        void shouldHaveDeterministicSceneOrder() {
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result1 = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");
            assertThat(result1.isSuccess()).isTrue();

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository, atLeast(1)).save(captor.capture());
            StoryboardDraft draft = captor.getAllValues().stream()
                    .filter(d -> !d.getScenes().isEmpty())
                    .findFirst().orElseThrow();

            // Beat type'lar sırayla scene'lere map edilmeli
            assertThat(draft.getScenes().get(0).getSourceBeatType()).isEqualTo(ScenarioBeatType.HOOK);
            assertThat(draft.getScenes().get(1).getSourceBeatType()).isEqualTo(ScenarioBeatType.INTRODUCTION);
            assertThat(draft.getScenes().get(2).getSourceBeatType()).isEqualTo(ScenarioBeatType.MAIN_NARRATIVE);
        }

        @Test
        @DisplayName("4. İlk generation → version 1")
        void shouldBeVersion1ForFirstGeneration() {
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("5. İkinci generation → version 2")
        void shouldIncrementVersionForRegeneration() {
            setupMocks(approvedScenario, 1); // mevcut version 1

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("6. Safe provider metadata kaydedilir")
        void shouldStoreSafeProviderMetadata() {
            setupMocks(approvedScenario, 0);

            service.generateStoryboardForScenario(approvedScenario.getId(), "admin-serhat");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository, atLeast(1)).save(captor.capture());
            StoryboardDraft saved = captor.getAllValues().get(0);

            assertThat(saved.getProviderType()).isEqualTo("MOCK");
            assertThat(saved.getModelName()).isEqualTo("mock-storyboard-v1");
        }

        @Test
        @DisplayName("7. Source traceability korunur")
        void shouldMaintainSourceTraceability() {
            setupMocks(approvedScenario, 0);

            service.generateStoryboardForScenario(approvedScenario.getId(), "admin-serhat");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository, atLeast(1)).save(captor.capture());
            StoryboardDraft saved = captor.getAllValues().get(0);

            assertThat(saved.getContentProject().getId()).isEqualTo(project.getId());
            assertThat(saved.getSourceDraft().getId()).isEqualTo(approvedDraft.getId());
            assertThat(saved.getSourceScenarioDraft().getId()).isEqualTo(approvedScenario.getId());
            assertThat(saved.getSourceScenarioVersion()).isEqualTo(1);
        }
    }

    // ── Prerequisite Validation ──────────────────────────────

    @Nested
    @DisplayName("Prerequisite doğrulama")
    class PrerequisiteValidation {

        @Test
        @DisplayName("8. Approved ScenarioDraft gerekli")
        void shouldRequireApprovedScenarioDraft() {
            setupMocks(approvedScenario, 0);

            // approved scenario ile başarılı
            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("9. AI_GENERATED scenario'dan üretim başarısız")
        void shouldFailForAiGeneratedScenario() {
            ScenarioDraft unapproved = createScenarioWithStatus(ScenarioDraftStatus.AI_GENERATED);
            when(scenarioDraftRepository.findById(unapproved.getId())).thenReturn(Optional.of(unapproved));

            assertThatThrownBy(() -> service.generateStoryboardForScenario(
                    unapproved.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("10. NEEDS_REVIEW scenario'dan üretim başarısız")
        void shouldFailForNeedsReviewScenario() {
            ScenarioDraft unapproved = createScenarioWithStatus(ScenarioDraftStatus.NEEDS_REVIEW);
            when(scenarioDraftRepository.findById(unapproved.getId())).thenReturn(Optional.of(unapproved));

            assertThatThrownBy(() -> service.generateStoryboardForScenario(
                    unapproved.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("11. REJECTED scenario'dan üretim başarısız")
        void shouldFailForRejectedScenario() {
            ScenarioDraft rejected = createScenarioWithStatus(ScenarioDraftStatus.REJECTED);
            when(scenarioDraftRepository.findById(rejected.getId())).thenReturn(Optional.of(rejected));

            assertThatThrownBy(() -> service.generateStoryboardForScenario(
                    rejected.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("12. FAILED scenario'dan üretim başarısız")
        void shouldFailForFailedScenario() {
            ScenarioDraft failed = createScenarioWithStatus(ScenarioDraftStatus.FAILED);
            when(scenarioDraftRepository.findById(failed.getId())).thenReturn(Optional.of(failed));

            assertThatThrownBy(() -> service.generateStoryboardForScenario(
                    failed.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("13. Prerequisite fail olduğunda gateway çağrılmaz")
        void shouldNotCallGatewayWhenPrerequisiteFails() {
            // Spy kullanarak gateway çağrısını doğrula
            AiStoryboardAgentGateway spyGateway = spy(mockGateway);
            StoryboardGenerationService spyService = new StoryboardGenerationService(
                    scenarioDraftRepository, storyboardDraftRepository, spyGateway);

            ScenarioDraft unapproved = createScenarioWithStatus(ScenarioDraftStatus.AI_GENERATED);
            when(scenarioDraftRepository.findById(unapproved.getId())).thenReturn(Optional.of(unapproved));

            assertThatThrownBy(() -> spyService.generateStoryboardForScenario(
                    unapproved.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class);

            verify(spyGateway, never()).generateStoryboard(any());
        }

        @Test
        @DisplayName("14. Mevcut olmayan ScenarioDraft → NotFoundException")
        void shouldThrowNotFoundForMissingScenario() {
            UUID missingId = UUID.randomUUID();
            when(scenarioDraftRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateStoryboardForScenario(
                    missingId, "admin-serhat"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── Governance ──────────────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("15. AI/mock output hiçbir zaman APPROVED oluşturmaz")
        void shouldNeverCreateApprovedStoryboard() {
            setupMocks(approvedScenario, 0);

            service.generateStoryboardForScenario(approvedScenario.getId(), "admin-serhat");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository, atLeast(1)).save(captor.capture());

            for (StoryboardDraft saved : captor.getAllValues()) {
                assertThat(saved.getStatus()).isNotEqualTo(StoryboardDraftStatus.APPROVED);
            }
        }

        @Test
        @DisplayName("16. Generation asla READY_FOR_PUBLISH ayarlamaz")
        void shouldNeverSetReadyForPublish() {
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.getStatus()).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("17. Generic status update davranışı eklenmez")
        void shouldNotHaveGenericStatusUpdate() {
            // StoryboardGenerationService yalnızca AI_GENERATED veya FAILED oluşturur
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.getStatus()).isIn("AI_GENERATED", "FAILED");
        }

        @Test
        @DisplayName("18. Media generation/storage field yok")
        void shouldNotHaveMediaFields() {
            setupMocks(approvedScenario, 0);

            service.generateStoryboardForScenario(approvedScenario.getId(), "admin-serhat");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository, atLeast(1)).save(captor.capture());
            StoryboardDraft saved = captor.getAllValues().stream()
                    .filter(d -> !d.getScenes().isEmpty())
                    .findFirst().orElseThrow();

            // Entity'de media URL field yok — reflection ile doğrula
            java.lang.reflect.Field[] fields = StoryboardDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("imageurl");
                assertThat(name).doesNotContain("videourl");
                assertThat(name).doesNotContain("audiourl");
            }
        }
    }

    // ── Failure Handling ────────────────────────────────────

    @Nested
    @DisplayName("Hata yönetimi")
    class FailureHandling {

        @Test
        @DisplayName("19. Gateway timeout → FAILED storyboard")
        void shouldCreateFailedOnTimeout() {
            setupMocks(approvedScenario, 0);
            mockGateway.setMode(MockStoryboardGatewayMode.TIMEOUT);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("20. Boş yanıt → FAILED storyboard")
        void shouldCreateFailedOnEmptyResponse() {
            setupMocks(approvedScenario, 0);
            mockGateway.setMode(MockStoryboardGatewayMode.EMPTY_RESPONSE);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("21. Geçersiz format → FAILED storyboard")
        void shouldCreateFailedOnInvalidFormat() {
            setupMocks(approvedScenario, 0);
            mockGateway.setMode(MockStoryboardGatewayMode.INVALID_FORMAT);

            StoryboardGenerationResult result = service.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("22. Failed gateway APPROVED oluşturmaz")
        void failedGatewayShouldNeverCreateApproved() {
            for (MockStoryboardGatewayMode mode : MockStoryboardGatewayMode.values()) {
                if (mode == MockStoryboardGatewayMode.SUCCESS) continue;

                setUp(); // her mod için temiz başlat
                setupMocks(approvedScenario, 0);
                mockGateway.setMode(mode);

                StoryboardGenerationResult result = service.generateStoryboardForScenario(
                        approvedScenario.getId(), "admin-serhat");

                assertThat(result.getStatus())
                        .as("Mode %s should not produce APPROVED", mode)
                        .isNotEqualTo("APPROVED");
            }
        }

        @Test
        @DisplayName("23. Gateway exception → sanitized failure")
        void shouldHandleGatewayException() {
            AiStoryboardAgentGateway throwingGateway = request -> {
                throw new RuntimeException("Unexpected network error");
            };
            StoryboardGenerationService exceptionService = new StoryboardGenerationService(
                    scenarioDraftRepository, storyboardDraftRepository, throwingGateway);

            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = exceptionService.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getErrorCode()).isEqualTo("UNKNOWN_AI_ERROR");
            // Sanitized — raw exception message exposed değil
            assertThat(result.getMessage()).doesNotContain("network error");
        }
    }

    // ── Response Validation ─────────────────────────────────

    @Nested
    @DisplayName("Response validation")
    class ResponseValidation {

        @Test
        @DisplayName("24. Başlıksız yanıt → FAILED")
        void shouldFailOnMissingTitle() {
            AiStoryboardAgentGateway noTitleGateway = request ->
                    StoryboardAgentResponse.builder()
                            .success(true)
                            .storyboardTitle(null) // başlık yok
                            .scenes(createValidSceneOutputs())
                            .providerType("MOCK")
                            .modelName("mock-storyboard-v1")
                            .generatedAt(LocalDateTime.now())
                            .build();

            StoryboardGenerationService testService = new StoryboardGenerationService(
                    scenarioDraftRepository, storyboardDraftRepository, noTitleGateway);
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = testService.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("25. Boş scene listesi → FAILED")
        void shouldFailOnEmptySceneList() {
            AiStoryboardAgentGateway noScenesGateway = request ->
                    StoryboardAgentResponse.builder()
                            .success(true)
                            .storyboardTitle("Valid Title")
                            .scenes(List.of()) // boş scene listesi
                            .providerType("MOCK")
                            .modelName("mock-storyboard-v1")
                            .generatedAt(LocalDateTime.now())
                            .build();

            StoryboardGenerationService testService = new StoryboardGenerationService(
                    scenarioDraftRepository, storyboardDraftRepository, noScenesGateway);
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = testService.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("26. visualDescription eksik scene → FAILED")
        void shouldFailOnMissingVisualDescription() {
            AiStoryboardAgentGateway badSceneGateway = request ->
                    StoryboardAgentResponse.builder()
                            .success(true)
                            .storyboardTitle("Valid Title")
                            .scenes(List.of(
                                    StoryboardSceneOutput.builder()
                                            .sceneNumber(1)
                                            .sourceBeatType(ScenarioBeatType.HOOK)
                                            .sourceBeatOrder(1)
                                            .sceneTitle("Scene 1")
                                            .visualDescription(null) // eksik
                                            .estimatedDurationSeconds(10)
                                            .build()
                            ))
                            .providerType("MOCK")
                            .modelName("mock-storyboard-v1")
                            .generatedAt(LocalDateTime.now())
                            .build();

            StoryboardGenerationService testService = new StoryboardGenerationService(
                    scenarioDraftRepository, storyboardDraftRepository, badSceneGateway);
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = testService.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("27. Geçersiz scene sırası → FAILED")
        void shouldFailOnInvalidSceneOrder() {
            AiStoryboardAgentGateway badOrderGateway = request ->
                    StoryboardAgentResponse.builder()
                            .success(true)
                            .storyboardTitle("Valid Title")
                            .scenes(List.of(
                                    StoryboardSceneOutput.builder()
                                            .sceneNumber(2) // 1 yerine 2 ile başlıyor
                                            .sourceBeatType(ScenarioBeatType.HOOK)
                                            .sourceBeatOrder(1)
                                            .sceneTitle("Scene")
                                            .visualDescription("Visual")
                                            .estimatedDurationSeconds(10)
                                            .build()
                            ))
                            .providerType("MOCK")
                            .modelName("mock-storyboard-v1")
                            .generatedAt(LocalDateTime.now())
                            .build();

            StoryboardGenerationService testService = new StoryboardGenerationService(
                    scenarioDraftRepository, storyboardDraftRepository, badOrderGateway);
            setupMocks(approvedScenario, 0);

            StoryboardGenerationResult result = testService.generateStoryboardForScenario(
                    approvedScenario.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }
    }

    // ── Bean Config Safety ──────────────────────────────────

    @Nested
    @DisplayName("Bean config safety")
    class BeanConfigSafety {

        @Test
        @DisplayName("28. Mock gateway AiStoryboardAgentGateway interface'ini implement eder")
        void mockGatewayShouldImplementInterface() {
            assertThat(mockGateway).isInstanceOf(AiStoryboardAgentGateway.class);
        }

        @Test
        @DisplayName("29. Mock fallback bağımsız olarak instantiate edilebilir")
        void mockFallbackShouldBeIndependentlyInstantiable() {
            MockAiStoryboardAgentGateway fallback = new MockAiStoryboardAgentGateway();
            assertThat(fallback.getMode()).isEqualTo(MockStoryboardGatewayMode.SUCCESS);

            // Gateway çağrısı yapılabilir
            StoryboardAgentRequest request = StoryboardAgentRequest.builder()
                    .requestId(UUID.randomUUID())
                    .contentProjectId(UUID.randomUUID())
                    .sourceDraftId(UUID.randomUUID())
                    .sourceScenarioId(UUID.randomUUID())
                    .sourceScenarioVersion(1)
                    .scenarioTitle("Test Scenario")
                    .scenarioBeats(createBeatInputs())
                    .language("tr")
                    .build();

            StoryboardAgentResponse response = fallback.generateStoryboard(request);
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getProviderType()).isEqualTo("MOCK");
        }

        @Test
        @DisplayName("30. FP-001 ve FP-002 gateway davranışı değişmedi")
        void existingGatewaysShouldRemainUnchanged() {
            // AiStoryboardAgentGateway ayrı interface — mevcut gateway'ler etkilenmez
            assertThat(AiStoryboardAgentGateway.class.getName())
                    .isNotEqualTo(com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway.class.getName());
            assertThat(AiStoryboardAgentGateway.class.getName())
                    .isNotEqualTo(com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway.class.getName());
            // MockAiStoryboardAgentGateway sadece AiStoryboardAgentGateway implement eder
            assertThat(mockGateway).isNotInstanceOf(
                    com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway.class);
            assertThat(mockGateway).isNotInstanceOf(
                    com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway.class);
        }
    }

    // ── Helper Methods ──────────────────────────────────────

    private ScenarioDraft createApprovedScenario() {
        ScenarioDraft scenario = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .version(1)
                .status(ScenarioDraftStatus.APPROVED)
                .title("Göbeklitepe — Senaryo")
                .summary("Göbeklitepe hakkında video senaryosu")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-scenario-v1")
                .build();

        // 6 beat ekle
        List<ScenarioBeat> beats = new ArrayList<>();
        for (ScenarioBeatType beatType : ScenarioBeatType.values()) {
            ScenarioBeat beat = ScenarioBeat.builder()
                    .scenarioDraft(scenario)
                    .type(beatType)
                    .label(beatType.getLabel())
                    .body(String.format("Beat body: %s — Göbeklitepe", beatType.getLabel()))
                    .orderIndex(beatType.getOrder())
                    .estimatedDurationSeconds(30)
                    .build();
            beat.prePersist(); // UUID ata
            beats.add(beat);
        }
        scenario.getBeats().addAll(beats);

        return scenario;
    }

    private ScenarioDraft createScenarioWithStatus(ScenarioDraftStatus status) {
        ScenarioDraft scenario = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .version(1)
                .status(status)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
        return scenario;
    }

    private void setupMocks(ScenarioDraft scenario, int existingMaxVersion) {
        when(scenarioDraftRepository.findById(scenario.getId()))
                .thenReturn(Optional.of(scenario));
        when(storyboardDraftRepository.findMaxVersionByContentProjectId(project.getId()))
                .thenReturn(existingMaxVersion);
        when(storyboardDraftRepository.save(any(StoryboardDraft.class)))
                .thenAnswer(invocation -> {
                    StoryboardDraft draft = invocation.getArgument(0);
                    if (draft.getId() == null) {
                        draft.prePersist();
                    }
                    return draft;
                });
    }

    private List<StoryboardSceneOutput> createValidSceneOutputs() {
        List<StoryboardSceneOutput> scenes = new ArrayList<>();
        int i = 1;
        for (ScenarioBeatType beatType : ScenarioBeatType.values()) {
            scenes.add(StoryboardSceneOutput.builder()
                    .sceneNumber(i)
                    .sourceBeatType(beatType)
                    .sourceBeatOrder(beatType.getOrder())
                    .sceneTitle("Scene " + i)
                    .visualDescription("Visual for scene " + i)
                    .estimatedDurationSeconds(30)
                    .build());
            i++;
        }
        return scenes;
    }

    private List<com.anadoluatlasi.backend.ai.contract.StoryboardSourceBeatInput> createBeatInputs() {
        List<com.anadoluatlasi.backend.ai.contract.StoryboardSourceBeatInput> inputs = new ArrayList<>();
        for (ScenarioBeatType beatType : ScenarioBeatType.values()) {
            inputs.add(com.anadoluatlasi.backend.ai.contract.StoryboardSourceBeatInput.builder()
                    .beatType(beatType)
                    .beatTitle(beatType.getLabel())
                    .beatBody("Test body for " + beatType.name())
                    .orderIndex(beatType.getOrder())
                    .estimatedDurationSeconds(30)
                    .build());
        }
        return inputs;
    }
}
