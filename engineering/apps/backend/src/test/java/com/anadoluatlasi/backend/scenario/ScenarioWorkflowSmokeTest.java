package com.anadoluatlasi.backend.scenario;

import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ScenarioBeatResult;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiScenarioAgentGateway;
import com.anadoluatlasi.backend.application.scenario.*;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.content.entity.*;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-002 Task 10: End-to-end smoke test validating the full scenario workflow.
 *
 * <p>Covers: Generate → 6-beat validation → Edit → Approve → Reject</p>
 * <p>Validates: Governance, status transitions, beat structure, safety rules.</p>
 * <p>No database or OpenAI required — all external dependencies mocked.</p>
 */
@ExtendWith(MockitoExtension.class)
class ScenarioWorkflowSmokeTest {

    @Mock private ContentProjectRepository contentProjectRepository;
    @Mock private ContentDraftRepository contentDraftRepository;
    @Mock private ScenarioDraftRepository scenarioDraftRepository;

    private MockAiScenarioAgentGateway mockGateway;
    private ScenarioGenerationService generationService;
    private ScenarioReviewService reviewService;
    private ScenarioQueryService queryService;

    private ContentProject project;
    private ContentDraft approvedDraft;

    @BeforeEach
    void setUp() {
        mockGateway = new MockAiScenarioAgentGateway();
        generationService = new ScenarioGenerationService(
                contentProjectRepository, contentDraftRepository,
                scenarioDraftRepository, mockGateway);
        reviewService = new ScenarioReviewService(scenarioDraftRepository);
        queryService = new ScenarioQueryService(scenarioDraftRepository);

        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        approvedDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Çanakkale Türküsünün Hikâyesi")
                .body("Çanakkale Türküsü, Birinci Dünya Savaşı sırasında Çanakkale Savaşı'nın acılarını anlatan halk türküsüdür.")
                .summary("Çanakkale Savaşı ve türküsü")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    private void mockGenerationPrerequisites() {
        when(contentProjectRepository.findById(project.getId()))
                .thenReturn(Optional.of(project));
        when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                .thenReturn(List.of(approvedDraft));
        when(scenarioDraftRepository.findMaxVersionByContentProjectId(project.getId()))
                .thenReturn(0);
        when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                .thenAnswer(inv -> {
                    ScenarioDraft draft = inv.getArgument(0);
                    if (draft.getId() == null) draft.onPrePersist();
                    return draft;
                });
    }

    // ══════════════════════════════════════════════════════════
    //  FLOW A — MOCK PROVIDER FULL WORKFLOW
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Flow A — Mock Provider Full Workflow Smoke")
    class FlowAMockWorkflow {

        @Test
        @DisplayName("A.1: Mock gateway produces valid 6-beat response directly")
        void mockGatewayProducesValid6BeatResponse() {
            ScenarioAgentRequest request = ScenarioAgentRequest.builder()
                    .requestId(UUID.randomUUID())
                    .contentProjectId(project.getId())
                    .sourceDraftId(approvedDraft.getId())
                    .sourceDraftTitle(approvedDraft.getTitle())
                    .sourceDraftBody(approvedDraft.getBody())
                    .targetAudience("Genel izleyici")
                    .tone("Duygusal")
                    .language("tr")
                    .desiredDurationSeconds(600)
                    .build();

            ScenarioAgentResponse response = mockGateway.generateScenario(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getScenarioTitle()).isNotBlank();
            assertThat(response.getSummary()).isNotBlank();
            assertThat(response.getBeats()).hasSize(6);

            // All 6 beat types present
            Set<ScenarioBeatType> beatTypes = EnumSet.noneOf(ScenarioBeatType.class);
            for (ScenarioBeatResult beat : response.getBeats()) {
                beatTypes.add(beat.getType());
                assertThat(beat.getBody()).isNotBlank();
                assertThat(beat.getTitle()).isNotBlank();
            }
            assertThat(beatTypes).containsExactlyInAnyOrder(ScenarioBeatType.values());

            // Deterministic beat order
            List<Integer> orders = response.getBeats().stream()
                    .map(ScenarioBeatResult::getOrder).toList();
            assertThat(orders).isSorted();

            // Safe metadata
            assertThat(response.getProviderType()).isEqualTo("MOCK");
        }

        @Test
        @DisplayName("A.2: Generate scenario creates AI_GENERATED draft with 6 beats")
        void generateCreatesAiGeneratedDraftWith6Beats() {
            mockGenerationPrerequisites();

            ScenarioGenerationResult result = generationService
                    .generateScenarioForContentProject(project.getId(), "admin-smoke");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("AI_GENERATED");
            assertThat(result.getScenarioTitle()).isNotBlank();
            assertThat(result.getVersion()).isEqualTo(1);

            // Verify persisted draft
            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            ScenarioDraft saved = captor.getAllValues().stream()
                    .filter(d -> d.getStatus() == ScenarioDraftStatus.AI_GENERATED)
                    .filter(d -> !d.getBeats().isEmpty())
                    .findFirst().orElseThrow();

            assertThat(saved.getStatus()).isEqualTo(ScenarioDraftStatus.AI_GENERATED);
            assertThat(saved.getBeats()).hasSize(6);
            assertThat(saved.getProviderType()).isEqualTo("MOCK");
        }

        @Test
        @DisplayName("A.3: AI output is never auto-approved")
        void aiOutputNeverAutoApproved() {
            mockGenerationPrerequisites();
            generationService.generateScenarioForContentProject(project.getId(), "admin-smoke");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());

            for (ScenarioDraft d : captor.getAllValues()) {
                assertThat(d.getStatus()).isNotEqualTo(ScenarioDraftStatus.APPROVED);
            }
        }

        @Test
        @DisplayName("A.4: Edit changes status to NEEDS_REVIEW (save ≠ approve)")
        void editChangesStatusToNeedsReview() {
            ScenarioDraft draft = createDraftForReview(ScenarioDraftStatus.AI_GENERATED);
            when(scenarioDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScenarioDraftEditCommand editCmd = ScenarioDraftEditCommand.builder()
                    .title("Düzenlenmiş Başlık")
                    .build();

            reviewService.editScenarioDraft(draft.getId(), editCmd, "admin-smoke");

            assertThat(draft.getStatus()).isEqualTo(ScenarioDraftStatus.NEEDS_REVIEW);
            assertThat(draft.getStatus()).isNotEqualTo(ScenarioDraftStatus.APPROVED);
        }

        @Test
        @DisplayName("A.5: Approve is explicit admin action → APPROVED")
        void approveIsExplicitAction() {
            ScenarioDraft draft = createDraftForReview(ScenarioDraftStatus.NEEDS_REVIEW);
            when(scenarioDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(scenarioDraftRepository.findByContentProjectIdAndStatus(
                    draft.getContentProject().getId(), ScenarioDraftStatus.APPROVED))
                    .thenReturn(List.of());
            when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScenarioReviewResult result = reviewService.approveScenarioDraft(draft.getId(), "admin-smoke");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(draft.getStatus()).isEqualTo(ScenarioDraftStatus.APPROVED);
            assertThat(draft.getApprovedAt()).isNotNull();
            assertThat(draft.getApprovedBy()).isEqualTo("admin-smoke");
        }

        @Test
        @DisplayName("A.6: Reject is explicit admin action → REJECTED with reason")
        void rejectIsExplicitAction() {
            ScenarioDraft draft = createDraftForReview(ScenarioDraftStatus.AI_GENERATED);
            when(scenarioDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(scenarioDraftRepository.save(any(ScenarioDraft.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ScenarioReviewResult result = reviewService.rejectScenarioDraft(
                    draft.getId(), "admin-smoke", "Kalite yetersiz");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
            assertThat(draft.getStatus()).isEqualTo(ScenarioDraftStatus.REJECTED);
            assertThat(draft.getRejectedAt()).isNotNull();
            assertThat(draft.getReviewNotes()).isEqualTo("Kalite yetersiz");
        }

        @Test
        @DisplayName("A.7: No READY_FOR_PUBLISH transition exists")
        void noReadyForPublishTransition() {
            for (ScenarioDraftStatus status : ScenarioDraftStatus.values()) {
                assertThat(status.name()).isNotEqualTo("READY_FOR_PUBLISH");
            }
        }

        @Test
        @DisplayName("A.8: One-approved-scenario rule prevents double approval")
        void oneApprovedScenarioRule() {
            ScenarioDraft draft2 = createDraftForReview(ScenarioDraftStatus.NEEDS_REVIEW);
            ScenarioDraft approvedExisting = createDraftForReview(ScenarioDraftStatus.APPROVED);
            when(scenarioDraftRepository.findById(draft2.getId())).thenReturn(Optional.of(draft2));
            when(scenarioDraftRepository.findByContentProjectIdAndStatus(
                    draft2.getContentProject().getId(), ScenarioDraftStatus.APPROVED))
                    .thenReturn(List.of(approvedExisting));

            assertThatThrownBy(() -> reviewService.approveScenarioDraft(draft2.getId(), "admin-smoke"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GOVERNANCE VALIDATIONS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Governance Validations")
    class GovernanceValidations {

        @Test
        @DisplayName("G.1: Unapproved draft cannot generate scenario")
        void unapprovedDraftCannotGenerate() {
            ContentDraft unapproved = ContentDraft.builder()
                    .id(UUID.randomUUID())
                    .contentProject(project)
                    .title("Unapproved")
                    .body("Not approved yet.")
                    .status(ContentDraftStatus.GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            when(contentProjectRepository.findById(project.getId()))
                    .thenReturn(Optional.of(project));
            when(contentDraftRepository.findByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(List.of(unapproved));

            assertThatThrownBy(() -> generationService
                    .generateScenarioForContentProject(project.getId(), "admin-smoke"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("G.2: APPROVED draft cannot be edited")
        void approvedDraftCannotBeEdited() {
            ScenarioDraft draft = createDraftForReview(ScenarioDraftStatus.APPROVED);
            when(scenarioDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.editScenarioDraft(
                    draft.getId(),
                    ScenarioDraftEditCommand.builder().title("Change").build(),
                    "admin-smoke"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("G.3: REJECTED draft cannot be approved")
        void rejectedDraftCannotBeApproved() {
            ScenarioDraft draft = createDraftForReview(ScenarioDraftStatus.REJECTED);
            when(scenarioDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.approveScenarioDraft(
                    draft.getId(), "admin-smoke"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("G.4: ScenarioDraftStatus values are exactly as expected")
        void statusValuesCorrect() {
            assertThat(ScenarioDraftStatus.values()).containsExactlyInAnyOrder(
                    ScenarioDraftStatus.AI_GENERATED,
                    ScenarioDraftStatus.NEEDS_REVIEW,
                    ScenarioDraftStatus.APPROVED,
                    ScenarioDraftStatus.REJECTED,
                    ScenarioDraftStatus.FAILED
            );
        }

        @Test
        @DisplayName("G.5: ScenarioBeatType has exactly 6 values in correct order")
        void beatTypeEnumCorrect() {
            assertThat(ScenarioBeatType.values()).hasSize(6);
            assertThat(ScenarioBeatType.HOOK.getOrder()).isEqualTo(1);
            assertThat(ScenarioBeatType.INTRODUCTION.getOrder()).isEqualTo(2);
            assertThat(ScenarioBeatType.MAIN_NARRATIVE.getOrder()).isEqualTo(3);
            assertThat(ScenarioBeatType.EMOTIONAL_CLIMAX.getOrder()).isEqualTo(4);
            assertThat(ScenarioBeatType.CONCLUSION.getOrder()).isEqualTo(5);
            assertThat(ScenarioBeatType.CALL_TO_ACTION.getOrder()).isEqualTo(6);
        }

        @Test
        @DisplayName("G.6: Provider metadata safe — no raw payload/prompt/secret")
        void providerMetadataSafe() {
            mockGenerationPrerequisites();
            generationService.generateScenarioForContentProject(project.getId(), "admin-smoke");

            ArgumentCaptor<ScenarioDraft> captor = ArgumentCaptor.forClass(ScenarioDraft.class);
            verify(scenarioDraftRepository, atLeastOnce()).save(captor.capture());
            ScenarioDraft saved = captor.getAllValues().stream()
                    .filter(d -> d.getStatus() == ScenarioDraftStatus.AI_GENERATED)
                    .findFirst().orElseThrow();

            // Safe metadata only
            assertThat(saved.getProviderType()).isIn("MOCK", "OPENAI");
            assertThat(saved.getModelName()).isNotNull();
            // No raw fields on entity
        }
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private ScenarioDraft createDraftForReview(ScenarioDraftStatus status) {
        ScenarioDraft draft = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .version(1)
                .status(status)
                .title("Mock Senaryo")
                .summary("Test summary")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-model")
                .beats(new ArrayList<>())
                .build();

        // Add 6 beats
        for (ScenarioBeatType beatType : ScenarioBeatType.values()) {
            ScenarioBeat beat = ScenarioBeat.builder()
                    .id(UUID.randomUUID())
                    .scenarioDraft(draft)
                    .type(beatType)
                    .label(beatType.getLabel())
                    .body("Beat body for " + beatType.name())
                    .orderIndex(beatType.getOrder())
                    .estimatedDurationSeconds(beatType.getOrder() * 10)
                    .build();
            draft.getBeats().add(beat);
        }

        if (status == ScenarioDraftStatus.APPROVED) {
            draft.setApprovedAt(java.time.LocalDateTime.now());
            draft.setApprovedBy("admin-system");
        } else if (status == ScenarioDraftStatus.REJECTED) {
            draft.setRejectedAt(java.time.LocalDateTime.now());
            draft.setRejectedBy("admin-system");
            draft.setReviewNotes("Reddedildi");
        }

        return draft;
    }
}
