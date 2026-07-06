package com.anadoluatlasi.backend.application.platformpackage;

import com.anadoluatlasi.backend.ai.gateway.AiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockPlatformPackageGatewayMode;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.review.GenerationReviewHandoffAdapter;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import com.anadoluatlasi.backend.platformpackage.repository.PlatformPackageDraftRepository;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-004: PlatformPackageGenerationService unit testleri.
 *
 * <p>Mock gateway kullanarak generation, prerequisite validation,
 * governance kuralları, all-or-nothing failure, versioning ve
 * bean config testlerini kapsar.</p>
 */
@ExtendWith(MockitoExtension.class)
class PlatformPackageGenerationServiceTest {

    @Mock
    private StoryboardDraftRepository storyboardDraftRepository;

    @Mock
    private PlatformPackageDraftRepository platformPackageDraftRepository;

    @Mock
    private GenerationReviewHandoffAdapter generationReviewHandoffAdapter;

    private MockAiPlatformPackageAgentGateway mockGateway;
    private PlatformPackageGenerationService service;
    private ObjectMapper objectMapper;

    private ContentProject project;
    private ContentDraft contentDraft;
    private ScenarioDraft scenarioDraft;
    private StoryboardDraft approvedStoryboard;

    @BeforeEach
    void setUp() {
        mockGateway = new MockAiPlatformPackageAgentGateway();
        objectMapper = new ObjectMapper();
        service = new PlatformPackageGenerationService(
                storyboardDraftRepository, platformPackageDraftRepository,
                mockGateway, objectMapper,
                generationReviewHandoffAdapter);

        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        contentDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Test Draft")
                .body("Test body")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        scenarioDraft = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(contentDraft)
                .version(1)
                .status(ScenarioDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        approvedStoryboard = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(contentDraft)
                .sourceScenarioDraft(scenarioDraft)
                .sourceScenarioVersion(1)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .title("Truva Atı Storyboard")
                .summary("Truva Atı hikayesi")
                .targetPlatform("YouTube")
                .aspectRatio("16:9")
                .estimatedDurationSeconds(600)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-storyboard-v1")
                .build();

        // 3 scene ekle
        List<StoryboardScene> scenes = new ArrayList<>();
        scenes.add(buildScene(approvedStoryboard, 1, ScenarioBeatType.HOOK, "Hook", "Hook görseli"));
        scenes.add(buildScene(approvedStoryboard, 2, ScenarioBeatType.MAIN_NARRATIVE, "Ana", "Ana görseli"));
        scenes.add(buildScene(approvedStoryboard, 3, ScenarioBeatType.CALL_TO_ACTION, "CTA", "CTA görseli"));
        approvedStoryboard.getScenes().addAll(scenes);
    }

    // ── Successful Generation ───────────────────────────────

    @Nested
    @DisplayName("Başarılı paket üretimi")
    class SuccessfulGeneration {

        @BeforeEach
        void setupMocks() {
            when(storyboardDraftRepository.findById(approvedStoryboard.getId()))
                    .thenReturn(Optional.of(approvedStoryboard));
            when(platformPackageDraftRepository.findMaxVersionByProjectAndStoryboardAndType(
                    any(), any(), any())).thenReturn(0);
            when(platformPackageDraftRepository.save(any(PlatformPackageDraft.class)))
                    .thenAnswer(inv -> {
                        PlatformPackageDraft d = inv.getArgument(0);
                        if (d.getId() == null) d.onPrePersist();
                        return d;
                    });
        }

        @Test
        @DisplayName("Approved StoryboardDraft'tan 4 platform paketi üretilir")
        void shouldGenerateFourPackages() {
            PlatformPackageGenerationResult result = service.generatePlatformPackages(
                    approvedStoryboard.getId(), "admin-test");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPackageCount()).isEqualTo(4);
            assertThat(result.getGeneratedPackageIds()).hasSize(4);
        }

        @Test
        @DisplayName("Üretilen paketler AI_GENERATED statusünde")
        void shouldHaveAiGeneratedStatus() {
            service.generatePlatformPackages(approvedStoryboard.getId(), "admin-test");

            // verify save called 4+ times (at least once per package)
            verify(platformPackageDraftRepository, org.mockito.Mockito.atLeast(4))
                    .save(org.mockito.ArgumentMatchers.argThat(draft ->
                            draft.getStatus() == PlatformPackageStatus.AI_GENERATED));
        }

        @Test
        @DisplayName("Tüm 4 platform tipi üretilir")
        void shouldIncludeAllFourPlatformTypes() {
            service.generatePlatformPackages(approvedStoryboard.getId(), "admin-test");

            // Capture all saved drafts
            var captor = org.mockito.ArgumentCaptor.forClass(PlatformPackageDraft.class);
            verify(platformPackageDraftRepository, org.mockito.Mockito.atLeast(4)).save(captor.capture());

            Set<PlatformPackageType> savedTypes = captor.getAllValues().stream()
                    .map(PlatformPackageDraft::getPackageType)
                    .collect(Collectors.toSet());

            assertThat(savedTypes).containsExactlyInAnyOrder(
                    PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageType.YOUTUBE_SHORTS,
                    PlatformPackageType.INSTAGRAM_REELS,
                    PlatformPackageType.FACEBOOK_REELS
            );
        }

        @Test
        @DisplayName("Version 1'den başlar")
        void shouldStartFromVersion1() {
            service.generatePlatformPackages(approvedStoryboard.getId(), "admin-test");

            var captor = org.mockito.ArgumentCaptor.forClass(PlatformPackageDraft.class);
            verify(platformPackageDraftRepository, org.mockito.Mockito.atLeast(4)).save(captor.capture());

            captor.getAllValues().stream()
                    .filter(d -> d.getVersion() != null)
                    .forEach(d -> assertThat(d.getVersion()).isEqualTo(1));
        }

        @Test
        @DisplayName("Regeneration version'ı artırır")
        void shouldIncrementVersionOnRegeneration() {
            when(platformPackageDraftRepository.findMaxVersionByProjectAndStoryboardAndType(
                    any(), any(), any())).thenReturn(2);

            service.generatePlatformPackages(approvedStoryboard.getId(), "admin-test");

            var captor = org.mockito.ArgumentCaptor.forClass(PlatformPackageDraft.class);
            verify(platformPackageDraftRepository, org.mockito.Mockito.atLeast(4)).save(captor.capture());

            captor.getAllValues().stream()
                    .filter(d -> d.getVersion() != null)
                    .forEach(d -> assertThat(d.getVersion()).isEqualTo(3));
        }

        @Test
        @DisplayName("Kaynak referanslar doğru set edilir")
        void shouldSetSourceReferences() {
            service.generatePlatformPackages(approvedStoryboard.getId(), "admin-test");

            var captor = org.mockito.ArgumentCaptor.forClass(PlatformPackageDraft.class);
            verify(platformPackageDraftRepository, org.mockito.Mockito.atLeast(4)).save(captor.capture());

            captor.getAllValues().forEach(d -> {
                assertThat(d.getContentProject().getId()).isEqualTo(project.getId());
                assertThat(d.getSourceStoryboardDraft().getId()).isEqualTo(approvedStoryboard.getId());
            });
        }

        @Test
        @DisplayName("Provider metadata safe set edilir")
        void shouldSetSafeProviderMetadata() {
            service.generatePlatformPackages(approvedStoryboard.getId(), "admin-test");

            var captor = org.mockito.ArgumentCaptor.forClass(PlatformPackageDraft.class);
            verify(platformPackageDraftRepository, org.mockito.Mockito.atLeast(4)).save(captor.capture());

            captor.getAllValues().forEach(d -> {
                assertThat(d.getProviderType()).isEqualTo("MOCK");
                assertThat(d.getModelName()).isEqualTo("mock-platform-package-v1");
                assertThat(d.getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
            });
        }
    }

    // ── Prerequisite Validation ──────────────────────────────

    @Nested
    @DisplayName("Prerequisite doğrulama")
    class PrerequisiteValidation {

        @Test
        @DisplayName("Mevcut olmayan StoryboardDraft → NotFoundException")
        void shouldFailOnMissingStoryboard() {
            UUID missingId = UUID.randomUUID();
            when(storyboardDraftRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generatePlatformPackages(missingId, "admin"))
                    .isInstanceOf(NotFoundException.class);

            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("AI_GENERATED StoryboardDraft → BusinessException")
        void shouldFailOnAiGeneratedStoryboard() {
            testInvalidStoryboardStatus(StoryboardDraftStatus.AI_GENERATED);
        }

        @Test
        @DisplayName("NEEDS_REVIEW StoryboardDraft → BusinessException")
        void shouldFailOnNeedsReviewStoryboard() {
            testInvalidStoryboardStatus(StoryboardDraftStatus.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("REJECTED StoryboardDraft → BusinessException")
        void shouldFailOnRejectedStoryboard() {
            testInvalidStoryboardStatus(StoryboardDraftStatus.REJECTED);
        }

        @Test
        @DisplayName("FAILED StoryboardDraft → BusinessException")
        void shouldFailOnFailedStoryboard() {
            testInvalidStoryboardStatus(StoryboardDraftStatus.FAILED);
        }

        @Test
        @DisplayName("Scene'siz StoryboardDraft → BusinessException")
        void shouldFailOnMissingScenes() {
            StoryboardDraft noScenes = StoryboardDraft.builder()
                    .id(UUID.randomUUID())
                    .contentProject(project)
                    .sourceDraft(contentDraft)
                    .sourceScenarioDraft(scenarioDraft)
                    .sourceScenarioVersion(1)
                    .version(1)
                    .status(StoryboardDraftStatus.APPROVED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();
            // scenes list is empty by default

            when(storyboardDraftRepository.findById(noScenes.getId()))
                    .thenReturn(Optional.of(noScenes));

            assertThatThrownBy(() -> service.generatePlatformPackages(noScenes.getId(), "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("scene");

            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Prerequisite hatasında gateway çağrılmaz")
        void shouldNotCallGatewayOnPrerequisiteFailure() {
            StoryboardDraft rejected = createStoryboardWithStatus(StoryboardDraftStatus.REJECTED);
            when(storyboardDraftRepository.findById(rejected.getId()))
                    .thenReturn(Optional.of(rejected));

            assertThatThrownBy(() -> service.generatePlatformPackages(rejected.getId(), "admin"))
                    .isInstanceOf(BusinessException.class);

            // Gateway should not have been called — no way to verify directly with real mock,
            // but no save should have been called
            verify(platformPackageDraftRepository, never()).save(any());
        }

        private void testInvalidStoryboardStatus(StoryboardDraftStatus invalidStatus) {
            StoryboardDraft invalid = createStoryboardWithStatus(invalidStatus);
            when(storyboardDraftRepository.findById(invalid.getId()))
                    .thenReturn(Optional.of(invalid));

            assertThatThrownBy(() -> service.generatePlatformPackages(invalid.getId(), "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(invalidStatus.name());

            verify(platformPackageDraftRepository, never()).save(any());
        }
    }

    // ── Governance Rules ────────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("Üretilen paket asla APPROVED olmaz")
        void shouldNeverProduceApproved() {
            when(storyboardDraftRepository.findById(approvedStoryboard.getId()))
                    .thenReturn(Optional.of(approvedStoryboard));
            when(platformPackageDraftRepository.findMaxVersionByProjectAndStoryboardAndType(
                    any(), any(), any())).thenReturn(0);
            when(platformPackageDraftRepository.save(any(PlatformPackageDraft.class)))
                    .thenAnswer(inv -> {
                        PlatformPackageDraft d = inv.getArgument(0);
                        if (d.getId() == null) d.onPrePersist();
                        return d;
                    });

            service.generatePlatformPackages(approvedStoryboard.getId(), "admin");

            var captor = org.mockito.ArgumentCaptor.forClass(PlatformPackageDraft.class);
            verify(platformPackageDraftRepository, org.mockito.Mockito.atLeast(4)).save(captor.capture());

            captor.getAllValues().forEach(d ->
                    assertThat(d.getStatus()).isNotEqualTo(PlatformPackageStatus.APPROVED));
        }

        @Test
        @DisplayName("READY_FOR_PUBLISH status mevcut değil")
        void shouldNotHaveReadyForPublishStatus() {
            for (PlatformPackageStatus status : PlatformPackageStatus.values()) {
                assertThat(status.name()).isNotEqualTo("READY_FOR_PUBLISH");
            }
        }

        @Test
        @DisplayName("Entity'de publishing/media alanı yok")
        void shouldNotHavePublishingOrMediaFields() {
            java.lang.reflect.Field[] fields = PlatformPackageDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("publishedat");
                assertThat(name).doesNotContain("imageurl");
                assertThat(name).doesNotContain("videourl");
                assertThat(name).doesNotContain("rawprompt");
                assertThat(name).doesNotContain("apikey");
                assertThat(name).doesNotContain("secret");
            }
        }
    }

    // ── All-or-Nothing Failure ──────────────────────────────

    @Nested
    @DisplayName("All-or-nothing / failure handling")
    class FailureHandling {

        @BeforeEach
        void setupMocks() {
            when(storyboardDraftRepository.findById(approvedStoryboard.getId()))
                    .thenReturn(Optional.of(approvedStoryboard));
        }

        @Test
        @DisplayName("TIMEOUT → failure result")
        void shouldFailOnTimeout() {
            mockGateway.setMode(MockPlatformPackageGatewayMode.TIMEOUT);

            PlatformPackageGenerationResult result = service.generatePlatformPackages(
                    approvedStoryboard.getId(), "admin");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("EMPTY_RESPONSE → failure result")
        void shouldFailOnEmptyResponse() {
            mockGateway.setMode(MockPlatformPackageGatewayMode.EMPTY_RESPONSE);

            PlatformPackageGenerationResult result = service.generatePlatformPackages(
                    approvedStoryboard.getId(), "admin");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("INVALID_FORMAT → failure result")
        void shouldFailOnInvalidFormat() {
            mockGateway.setMode(MockPlatformPackageGatewayMode.INVALID_FORMAT);

            PlatformPackageGenerationResult result = service.generatePlatformPackages(
                    approvedStoryboard.getId(), "admin");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("UNSAFE_CONTENT → failure result")
        void shouldFailOnUnsafeContent() {
            mockGateway.setMode(MockPlatformPackageGatewayMode.UNSAFE_CONTENT);

            PlatformPackageGenerationResult result = service.generatePlatformPackages(
                    approvedStoryboard.getId(), "admin");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gateway exception → sanitized failure")
        void shouldHandleGatewayException() {
            // Use a gateway that throws
            AiPlatformPackageAgentGateway throwingGateway = request -> {
                throw new RuntimeException("Connection refused");
            };
            PlatformPackageGenerationService failService = new PlatformPackageGenerationService(
                    storyboardDraftRepository, platformPackageDraftRepository,
                    throwingGateway, objectMapper,
                    generationReviewHandoffAdapter);

            PlatformPackageGenerationResult result = failService.generatePlatformPackages(
                    approvedStoryboard.getId(), "admin");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNKNOWN_AI_ERROR");
            // Sanitized message — raw exception not exposed
            assertThat(result.getMessage()).doesNotContain("Connection refused");
            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure modlarında APPROVED status üretilmez")
        void failuresShouldNeverProduceApproved() {
            for (MockPlatformPackageGatewayMode mode : MockPlatformPackageGatewayMode.values()) {
                if (mode == MockPlatformPackageGatewayMode.SUCCESS) continue;
                mockGateway.setMode(mode);

                PlatformPackageGenerationResult result = service.generatePlatformPackages(
                        approvedStoryboard.getId(), "admin");

                assertThat(result.isSuccess()).isFalse();
            }
        }
    }

    // ── Bean Config Safety ──────────────────────────────────

    @Nested
    @DisplayName("Bean config safety")
    class BeanConfigSafety {

        @Test
        @DisplayName("Mock package gateway interface'i implement eder")
        void shouldImplementInterface() {
            assertThat(mockGateway).isInstanceOf(AiPlatformPackageAgentGateway.class);
        }

        @Test
        @DisplayName("Mock package gateway bağımsız olarak instantiate edilebilir")
        void shouldBeIndependentlyInstantiable() {
            MockAiPlatformPackageAgentGateway fresh = new MockAiPlatformPackageAgentGateway();
            assertThat(fresh.getMode()).isEqualTo(MockPlatformPackageGatewayMode.SUCCESS);
        }

        @Test
        @DisplayName("FP-001/FP-002/FP-003 gateway'lerinden ayrı")
        void shouldBeSeparateFromOtherGateways() {
            assertThat(AiPlatformPackageAgentGateway.class)
                    .isNotEqualTo(com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway.class);
            assertThat(AiPlatformPackageAgentGateway.class)
                    .isNotEqualTo(com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway.class);
            assertThat(AiPlatformPackageAgentGateway.class)
                    .isNotEqualTo(com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway.class);
        }
    }

    // ── Helper Methods ──────────────────────────────────────

    private StoryboardDraft createStoryboardWithStatus(StoryboardDraftStatus status) {
        StoryboardDraft draft = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(contentDraft)
                .sourceScenarioDraft(scenarioDraft)
                .sourceScenarioVersion(1)
                .version(1)
                .status(status)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
        // Add scenes for APPROVED only
        if (status == StoryboardDraftStatus.APPROVED) {
            draft.getScenes().add(buildScene(draft, 1, ScenarioBeatType.HOOK, "Test", "Test"));
        }
        return draft;
    }

    private StoryboardScene buildScene(StoryboardDraft draft, int number,
                                        ScenarioBeatType beatType, String title, String visual) {
        return StoryboardScene.builder()
                .id(UUID.randomUUID())
                .storyboardDraft(draft)
                .sceneNumber(number)
                .sourceBeatType(beatType)
                .sourceBeatOrder(beatType.getOrder())
                .sceneTitle(title)
                .visualDescription(visual)
                .narrationText("Narration " + number)
                .mood("Dramatik")
                .estimatedDurationSeconds(30)
                .build();
    }
}
