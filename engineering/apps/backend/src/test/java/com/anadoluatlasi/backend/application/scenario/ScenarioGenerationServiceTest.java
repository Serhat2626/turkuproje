package com.anadoluatlasi.backend.application.scenario;

import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ScenarioBeatResult;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockScenarioGatewayMode;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-002: ScenarioGenerationService unit testleri.
 *
 * <p>Kritik kurallar:</p>
 * <ul>
 *   <li>AI üretimi hiçbir zaman APPROVED senaryoya yol açmamalı</li>
 *   <li>Senaryo üretimi approved human-reviewed source draft gerektirir</li>
 *   <li>Raw prompt/payload/secret saklanmaz</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ScenarioGenerationServiceTest {

    @Mock
    private ContentProjectRepository contentProjectRepository;

    @Mock
    private ContentDraftRepository contentDraftRepository;

    @Mock
    private ScenarioDraftRepository scenarioDraftRepository;

    private MockAiScenarioAgentGateway mockGateway;
    private ScenarioGenerationService service;

    private ContentProject project;
    private ContentDraft approvedDraft;
    private ContentDraft unapprovedDraft;

    @BeforeEach
    void setUp() {
        mockGateway = new MockAiScenarioAgentGateway();
        service = new ScenarioGenerationService(
                contentProjectRepository,
                contentDraftRepository,
                scenarioDraftRepository,
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
                .body("Göbeklitepe, Şanlıurfa ilinde bulunan dünyanın bilinen en eski tapınak alanıdır. " +
                        "MÖ 10.000 yılına tarihlenen bu yapı, tarih anlayışımızı kökten değiştirmiştir. " +
                        "Avcı-toplayıcı toplulukların anıtsal yapılar inşa edebileceğini göstermiştir.")
                .summary("Göbeklitepe hakkında bilgilendirici metin")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        unapprovedDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Draft Başlık")
                .body("Draft body content.")
                .status(ContentDraftStatus.GENERATED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    // ── Başarılı Senaryo Üretimi ─────────────────────────────

    @Nested
    @DisplayName("Başarılı senaryo üretimi")
    class SuccessfulGeneration {

        @BeforeEach
        void setUpMocks() {
            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of(approvedDraft));
            when(scenarioDraftRepository.findMaxVersionByContentProjectId(project.getId()))
                    .thenReturn(0);
            when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> {
                        ScenarioDraft draft = inv.getArgument(0);
                        if (draft.getId() == null) {
                            draft.prePersist();
                        }
                        return draft;
                    });
        }

        @Test
        @DisplayName("Mock üretim ScenarioDraft AI_GENERATED status ile oluşturur")
        void shouldCreateScenarioDraftWithAiGeneratedStatus() {
            ScenarioGenerationResult result = service.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("AI_GENERATED");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            ScenarioDraft saved = captor.getAllValues().stream()
                    .filter(d -> d.getStatus() == ScenarioDraftStatus.AI_GENERATED)
                    .findFirst().orElseThrow();

            assertThat(saved.getStatus()).isEqualTo(ScenarioDraftStatus.AI_GENERATED);
            assertThat(saved.getStatus()).isNotEqualTo(ScenarioDraftStatus.APPROVED);
        }

        @Test
        @DisplayName("Mock üretim tam 6 beat oluşturur")
        void shouldCreateExactlySixBeats() {
            service.generateScenarioForContentProject(project.getId(), "admin-1");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            ScenarioDraft saved = captor.getAllValues().stream()
                    .filter(d -> d.getStatus() == ScenarioDraftStatus.AI_GENERATED)
                    .filter(d -> !d.getBeats().isEmpty())
                    .findFirst().orElseThrow();

            assertThat(saved.getBeats()).hasSize(6);
        }

        @Test
        @DisplayName("Beat tipleri ve sıralaması deterministik")
        void shouldHaveDeterministicBeatTypesAndOrder() {
            service.generateScenarioForContentProject(project.getId(), "admin-1");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            ScenarioDraft saved = captor.getAllValues().stream()
                    .filter(d -> d.getStatus() == ScenarioDraftStatus.AI_GENERATED)
                    .filter(d -> !d.getBeats().isEmpty())
                    .findFirst().orElseThrow();

            assertThat(saved.getBeats().get(0).getType()).isEqualTo(ScenarioBeatType.HOOK);
            assertThat(saved.getBeats().get(1).getType()).isEqualTo(ScenarioBeatType.INTRODUCTION);
            assertThat(saved.getBeats().get(2).getType()).isEqualTo(ScenarioBeatType.MAIN_NARRATIVE);
            assertThat(saved.getBeats().get(3).getType()).isEqualTo(ScenarioBeatType.EMOTIONAL_CLIMAX);
            assertThat(saved.getBeats().get(4).getType()).isEqualTo(ScenarioBeatType.CONCLUSION);
            assertThat(saved.getBeats().get(5).getType()).isEqualTo(ScenarioBeatType.CALL_TO_ACTION);

            assertThat(saved.getBeats().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(saved.getBeats().get(5).getOrderIndex()).isEqualTo(6);
        }

        @Test
        @DisplayName("İlk üretim version 1 olur")
        void shouldBeVersion1ForFirstGeneration() {
            ScenarioGenerationResult result = service.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("İkinci üretim version 2 olur")
        void shouldIncrementVersionForSecondGeneration() {
            when(scenarioDraftRepository.findMaxVersionByContentProjectId(project.getId()))
                    .thenReturn(1);

            ScenarioGenerationResult result = service.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Provider metadata sadece safe alanlar saklanır")
        void shouldOnlyStoreSafeProviderMetadata() {
            service.generateScenarioForContentProject(project.getId(), "admin-1");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            ScenarioDraft saved = captor.getAllValues().stream()
                    .filter(d -> d.getStatus() == ScenarioDraftStatus.AI_GENERATED)
                    .findFirst().orElseThrow();

            assertThat(saved.getProviderType()).isEqualTo("MOCK");
            assertThat(saved.getModelName()).isEqualTo("mock-scenario-v1");
        }

        @Test
        @DisplayName("Source draft referansı korunur (traceability)")
        void shouldMaintainSourceDraftTraceability() {
            service.generateScenarioForContentProject(project.getId(), "admin-1");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            ScenarioDraft saved = captor.getAllValues().stream()
                    .filter(d -> d.getStatus() == ScenarioDraftStatus.AI_GENERATED)
                    .findFirst().orElseThrow();

            assertThat(saved.getSourceDraft()).isEqualTo(approvedDraft);
            assertThat(saved.getContentProject()).isEqualTo(project);
        }
    }

    // ── Prerequisite Validation ──────────────────────────────

    @Nested
    @DisplayName("Prerequisite validation")
    class PrerequisiteValidation {

        @Test
        @DisplayName("Approved draft yoksa BusinessException fırlatır")
        void shouldThrowWhenNoApprovedDraft() {
            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of(unapprovedDraft));

            assertThatThrownBy(() ->
                    service.generateScenarioForContentProject(project.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("onaylanmış");

            // Gateway çağrılmamalı
            verify(scenarioDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Hiç draft yoksa BusinessException fırlatır")
        void shouldThrowWhenNoDrafts() {
            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of());

            assertThatThrownBy(() ->
                    service.generateScenarioForContentProject(project.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class);

            verify(scenarioDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Content Project bulunamazsa NotFoundException fırlatır")
        void shouldThrowWhenProjectNotFound() {
            UUID missingId = UUID.randomUUID();
            when(contentProjectRepository.findById(missingId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.generateScenarioForContentProject(missingId, "admin-1"))
                    .isInstanceOf(NotFoundException.class);

            verify(scenarioDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("GENERATED status'lü draft approved kabul edilmez")
        void shouldRejectGeneratedDraftAsSource() {
            ContentDraft generatedDraft = ContentDraft.builder()
                    .id(UUID.randomUUID())
                    .contentProject(project)
                    .title("AI Draft")
                    .body("AI generated content.")
                    .status(ContentDraftStatus.GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of(generatedDraft));

            assertThatThrownBy(() ->
                    service.generateScenarioForContentProject(project.getId(), "admin-1"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Governance Kuralları ─────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @BeforeEach
        void setUpMocks() {
            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of(approvedDraft));
            when(scenarioDraftRepository.findMaxVersionByContentProjectId(project.getId()))
                    .thenReturn(0);
            when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> {
                        ScenarioDraft draft = inv.getArgument(0);
                        if (draft.getId() == null) draft.prePersist();
                        return draft;
                    });
        }

        @Test
        @DisplayName("AI üretimi asla APPROVED scenario oluşturmaz")
        void aiShouldNeverCreateApprovedScenario() {
            ScenarioGenerationResult result = service.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isNotEqualTo("APPROVED");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            captor.getAllValues().forEach(draft ->
                    assertThat(draft.getStatus()).isNotEqualTo(ScenarioDraftStatus.APPROVED));
        }

        @Test
        @DisplayName("AI üretimi asla READY_FOR_PUBLISH yapmaz — ContentProject status değişmez")
        void aiShouldNeverSetReadyForPublish() {
            ContentProjectStatus originalStatus = project.getStatus();

            service.generateScenarioForContentProject(project.getId(), "admin-1");

            // ContentProject status değiştirilmemeli
            assertThat(project.getStatus()).isEqualTo(originalStatus);
        }
    }

    // ── Gateway Failure Handling ─────────────────────────────

    @Nested
    @DisplayName("Gateway failure handling")
    class GatewayFailureHandling {

        @BeforeEach
        void setUpMocks() {
            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of(approvedDraft));
            when(scenarioDraftRepository.findMaxVersionByContentProjectId(project.getId()))
                    .thenReturn(0);
            when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> {
                        ScenarioDraft draft = inv.getArgument(0);
                        if (draft.getId() == null) draft.prePersist();
                        return draft;
                    });
        }

        @Test
        @DisplayName("Gateway timeout FAILED scenario oluşturur")
        void shouldCreateFailedScenarioOnTimeout() {
            mockGateway.setMode(MockScenarioGatewayMode.TIMEOUT);

            ScenarioGenerationResult result = service.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getErrorCode()).isNotNull();
        }

        @Test
        @DisplayName("Gateway empty response FAILED scenario oluşturur")
        void shouldCreateFailedScenarioOnEmptyResponse() {
            mockGateway.setMode(MockScenarioGatewayMode.EMPTY_RESPONSE);

            ScenarioGenerationResult result = service.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("Gateway failure asla APPROVED scenario oluşturmaz")
        void failedGatewayShouldNeverCreateApprovedScenario() {
            mockGateway.setMode(MockScenarioGatewayMode.UNSAFE_CONTENT);

            ScenarioGenerationResult result = service.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isNotEqualTo("APPROVED");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            captor.getAllValues().forEach(draft ->
                    assertThat(draft.getStatus()).isNotEqualTo(ScenarioDraftStatus.APPROVED));
        }

        @Test
        @DisplayName("Gateway exception FAILED scenario oluşturur")
        void shouldHandleGatewayException() {
            // Exception fırlatan gateway
            AiScenarioAgentGateway throwingGateway = request -> {
                throw new RuntimeException("Connection refused");
            };
            ScenarioGenerationService exService = new ScenarioGenerationService(
                    contentProjectRepository, contentDraftRepository,
                    scenarioDraftRepository, throwingGateway);

            ScenarioGenerationResult result = exService.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
        }
    }

    // ── Invalid AI Output Validation ────────────────────────

    @Nested
    @DisplayName("Invalid AI output validation")
    class InvalidOutputValidation {

        @BeforeEach
        void setUpMocks() {
            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of(approvedDraft));
            when(scenarioDraftRepository.findMaxVersionByContentProjectId(project.getId()))
                    .thenReturn(0);
            when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> {
                        ScenarioDraft draft = inv.getArgument(0);
                        if (draft.getId() == null) draft.prePersist();
                        return draft;
                    });
        }

        @Test
        @DisplayName("Eksik beat'li response FAILED oluşturur")
        void shouldFailForMissingBeats() {
            // 5 beat döndüren gateway (6 yerine)
            AiScenarioAgentGateway partialGateway = request ->
                    ScenarioAgentResponse.builder()
                            .success(true)
                            .scenarioTitle("Test")
                            .beats(List.of(
                                    ScenarioBeatResult.builder()
                                            .type(ScenarioBeatType.HOOK)
                                            .title("Hook").body("Body").order(1).build(),
                                    ScenarioBeatResult.builder()
                                            .type(ScenarioBeatType.INTRODUCTION)
                                            .title("Intro").body("Body").order(2).build(),
                                    ScenarioBeatResult.builder()
                                            .type(ScenarioBeatType.MAIN_NARRATIVE)
                                            .title("Main").body("Body").order(3).build(),
                                    ScenarioBeatResult.builder()
                                            .type(ScenarioBeatType.EMOTIONAL_CLIMAX)
                                            .title("Climax").body("Body").order(4).build(),
                                    ScenarioBeatResult.builder()
                                            .type(ScenarioBeatType.CONCLUSION)
                                            .title("Conclusion").body("Body").order(5).build()
                            ))
                            .providerType("TEST")
                            .modelName("test-v1")
                            .generatedAt(LocalDateTime.now())
                            .build();

            ScenarioGenerationService customService = new ScenarioGenerationService(
                    contentProjectRepository, contentDraftRepository,
                    scenarioDraftRepository, partialGateway);

            ScenarioGenerationResult result = customService.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getStatus()).isNotEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Title'sız response FAILED oluşturur")
        void shouldFailForMissingTitle() {
            AiScenarioAgentGateway noTitleGateway = request ->
                    ScenarioAgentResponse.builder()
                            .success(true)
                            .scenarioTitle(null)
                            .beats(List.of())
                            .providerType("TEST")
                            .generatedAt(LocalDateTime.now())
                            .build();

            ScenarioGenerationService customService = new ScenarioGenerationService(
                    contentProjectRepository, contentDraftRepository,
                    scenarioDraftRepository, noTitleGateway);

            ScenarioGenerationResult result = customService.generateScenarioForContentProject(
                    project.getId(), "admin-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isNotEqualTo("APPROVED");
        }
    }
}
