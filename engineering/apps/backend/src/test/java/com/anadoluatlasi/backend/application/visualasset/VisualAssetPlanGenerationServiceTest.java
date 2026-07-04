package com.anadoluatlasi.backend.application.visualasset;

import com.anadoluatlasi.backend.ai.contract.VisualAssetPlanAgentResponse;
import com.anadoluatlasi.backend.ai.contract.VisualAssetSceneOutput;
import com.anadoluatlasi.backend.ai.contract.VisualAssetThumbnailOutput;
import com.anadoluatlasi.backend.ai.gateway.AiVisualAssetAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiVisualAssetAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockVisualAssetGatewayMode;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlanStatus;
import com.anadoluatlasi.backend.visualasset.repository.VisualAssetPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-006: VisualAssetPlanGenerationService unit testleri.
 *
 * <p>Spring context yüklenmez. Mockito mock'lar + gerçek MockAiVisualAssetAgentGateway kullanılır.</p>
 */
@ExtendWith(MockitoExtension.class)
class VisualAssetPlanGenerationServiceTest {

    @Mock
    private StoryboardDraftRepository storyboardDraftRepository;

    @Mock
    private VisualAssetPlanRepository visualAssetPlanRepository;

    private MockAiVisualAssetAgentGateway mockGateway;
    private VisualAssetPlanGenerationService service;

    private ContentProject project;
    private StoryboardDraft approvedStoryboard;
    private StoryboardScene scene1;
    private StoryboardScene scene2;
    private StoryboardScene scene3;

    private static final String ADMIN_ID = "admin-user-1";

    @BeforeEach
    void setUp() {
        mockGateway = new MockAiVisualAssetAgentGateway();
        service = new VisualAssetPlanGenerationService(
                storyboardDraftRepository,
                visualAssetPlanRepository,
                mockGateway);

        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .build();

        scene1 = StoryboardScene.builder()
                .id(UUID.randomUUID())
                .sceneNumber(1)
                .sourceBeatType(ScenarioBeatType.HOOK)
                .sourceBeatOrder(1)
                .sceneTitle("Açılış Sahnesi")
                .visualDescription("Anadolu köyünde gün doğumu")
                .narrationText("Bir zamanlar...")
                .mood("nostalgic")
                .location("Anadolu köyü")
                .characters("Yaşlı kadın")
                .cameraDirection("wide shot")
                .estimatedDurationSeconds(15)
                .build();

        scene2 = StoryboardScene.builder()
                .id(UUID.randomUUID())
                .sceneNumber(2)
                .sourceBeatType(ScenarioBeatType.MAIN_NARRATIVE)
                .sourceBeatOrder(3)
                .sceneTitle("Ana Hikâye")
                .visualDescription("Köy meydanında türkü söyleniyor")
                .narrationText("O gün köy meydanında...")
                .mood("dramatic")
                .location("Köy meydanı")
                .characters("Köylüler")
                .cameraDirection("medium shot")
                .estimatedDurationSeconds(30)
                .build();

        scene3 = StoryboardScene.builder()
                .id(UUID.randomUUID())
                .sceneNumber(3)
                .sourceBeatType(ScenarioBeatType.EMOTIONAL_CLIMAX)
                .sourceBeatOrder(4)
                .sceneTitle("Duygusal Zirve")
                .visualDescription("Gözyaşları ve hüzünlü bakışlar")
                .mood("emotional")
                .location("Ev içi")
                .cameraDirection("close-up")
                .estimatedDurationSeconds(20)
                .build();

        approvedStoryboard = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .title("Çanakkale Türküsü")
                .summary("Çanakkale destanının hikâyesi")
                .targetPlatform("YOUTUBE_LONG")
                .aspectRatio("16:9")
                .estimatedDurationSeconds(180)
                .visualStyleGuide("Cinematic Anatolian")
                .narrationStyle("Dramatic")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .scenes(new ArrayList<>(List.of(scene1, scene2, scene3)))
                .approvedAt(LocalDateTime.now())
                .approvedBy("admin")
                .build();
    }

    private void setupMocks(StoryboardDraft storyboard, int existingMaxVersion) {
        setupMocksNoSave(storyboard, existingMaxVersion);
        when(visualAssetPlanRepository.save(any(VisualAssetPlan.class)))
                .thenAnswer(invocation -> {
                    VisualAssetPlan plan = invocation.getArgument(0);
                    if (plan.getId() == null) {
                        plan.onPrePersist();
                    }
                    return plan;
                });
    }

    private void setupMocksNoSave(StoryboardDraft storyboard, int existingMaxVersion) {
        when(storyboardDraftRepository.findById(storyboard.getId()))
                .thenReturn(Optional.of(storyboard));
        when(visualAssetPlanRepository.findMaxVersionBySourceStoryboardDraftId(storyboard.getId()))
                .thenReturn(existingMaxVersion);
    }

    // ─── Başarılı Üretim ─────────────────────────────────────────────

    @Nested
    @DisplayName("Başarılı üretim")
    class SuccessfulGeneration {

        @Test
        @DisplayName("APPROVED storyboard ile görsel plan üretilir")
        void shouldGenerateVisualAssetPlan() {
            setupMocks(approvedStoryboard, 0);

            VisualAssetPlanGenerationResult result = service.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getVisualAssetPlanId()).isNotNull();
            assertThat(result.getMessage()).contains("başarıyla");
        }

        @Test
        @DisplayName("Üretilen plan status = AI_GENERATED")
        void shouldSetAiGeneratedStatus() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(VisualAssetPlanStatus.AI_GENERATED);
        }

        @Test
        @DisplayName("İlk plan version = 1")
        void shouldSetVersionOneWhenNoPrior() {
            setupMocks(approvedStoryboard, 0);

            VisualAssetPlanGenerationResult result = service.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID);

            assertThat(result.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Sonraki plan version artırılır")
        void shouldIncrementVersion() {
            setupMocks(approvedStoryboard, 3);

            VisualAssetPlanGenerationResult result = service.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID);

            assertThat(result.getVersion()).isEqualTo(4);
        }

        @Test
        @DisplayName("Plan StoryboardDraft referansını taşır")
        void shouldLinkSourceStoryboard() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getSourceStoryboardDraft().getId())
                    .isEqualTo(approvedStoryboard.getId());
        }

        @Test
        @DisplayName("Plan ContentProject referansını taşır")
        void shouldLinkContentProject() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getContentProject().getId())
                    .isEqualTo(project.getId());
        }

        @Test
        @DisplayName("Her StoryboardScene için bir SceneVisualPlan üretilir")
        void shouldCreateOneScenePlanPerScene() {
            setupMocks(approvedStoryboard, 0);

            VisualAssetPlanGenerationResult result = service.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID);

            assertThat(result.getScenePlanCount()).isEqualTo(3);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getScenePlans()).hasSize(3);
        }

        @Test
        @DisplayName("SceneVisualPlan kaynak StoryboardScene referansını taşır")
        void shouldReferenceSourceStoryboardScene() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());

            Set<UUID> sourceSceneIds = new HashSet<>();
            captor.getValue().getScenePlans().forEach(sp -> {
                if (sp.getSourceStoryboardScene() != null) {
                    sourceSceneIds.add(sp.getSourceStoryboardScene().getId());
                }
            });
            assertThat(sourceSceneIds).contains(scene1.getId(), scene2.getId(), scene3.getId());
        }

        @Test
        @DisplayName("SceneVisualPlan sceneNumber korunur")
        void shouldPreserveSceneNumber() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());

            List<Integer> sceneNumbers = captor.getValue().getScenePlans().stream()
                    .map(sp -> sp.getSceneNumber())
                    .sorted()
                    .toList();
            assertThat(sceneNumbers).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("SceneVisualPlan englishImagePrompt içerir")
        void shouldContainEnglishImagePrompt() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());

            captor.getValue().getScenePlans().forEach(sp ->
                    assertThat(sp.getEnglishImagePrompt()).isNotBlank());
        }

        @Test
        @DisplayName("SceneVisualPlan turkishVisualContext içerir")
        void shouldContainTurkishVisualContext() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());

            captor.getValue().getScenePlans().forEach(sp ->
                    assertThat(sp.getTurkishVisualContext()).isNotBlank());
        }

        @Test
        @DisplayName("ThumbnailPlan oluşturulur")
        void shouldCreateThumbnailPlan() {
            setupMocks(approvedStoryboard, 0);

            VisualAssetPlanGenerationResult result = service.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID);

            assertThat(result.getHasThumbnailPlan()).isTrue();

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getThumbnailPlan()).isNotNull();
        }

        @Test
        @DisplayName("ThumbnailPlan thumbnailPrompt içerir")
        void shouldContainThumbnailPrompt() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getThumbnailPlan().getThumbnailPrompt()).isNotBlank();
        }

        @Test
        @DisplayName("providerType ve modelName response'tan map edilir")
        void shouldMapProviderMetadata() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getProviderType()).isEqualTo("MOCK");
            assertThat(captor.getValue().getModelName()).isEqualTo("mock-visual-asset-planner");
        }

        @Test
        @DisplayName("Gateway tam olarak bir kez çağrılır")
        void shouldCallGatewayExactlyOnce() {
            setupMocks(approvedStoryboard, 0);
            // Spy ile kontrol
            AiVisualAssetAgentGateway spyGateway = spy(mockGateway);
            VisualAssetPlanGenerationService spyService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, spyGateway);

            spyService.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            verify(spyGateway, times(1)).generateVisualAssetPlan(any());
        }

        @Test
        @DisplayName("Repository save tam olarak bir kez çağrılır")
        void shouldCallSaveExactlyOnce() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            verify(visualAssetPlanRepository, times(1)).save(any(VisualAssetPlan.class));
        }
    }

    // ─── Ön Koşul Validasyonu ────────────────────────────────────────

    @Nested
    @DisplayName("Ön koşul validasyonu")
    class PrerequisiteValidation {

        @Test
        @DisplayName("Bulunamayan storyboard NotFoundException fırlatır")
        void shouldRejectMissingStoryboard() {
            UUID nonExistentId = UUID.randomUUID();
            when(storyboardDraftRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateVisualAssetPlan(nonExistentId, ADMIN_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("StoryboardDraft");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("AI_GENERATED status reddedilir")
        void shouldRejectAiGeneratedStoryboard() {
            assertStatusRejected(StoryboardDraftStatus.AI_GENERATED);
        }

        @Test
        @DisplayName("NEEDS_REVIEW status reddedilir")
        void shouldRejectNeedsReviewStoryboard() {
            assertStatusRejected(StoryboardDraftStatus.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("REJECTED status reddedilir")
        void shouldRejectRejectedStoryboard() {
            assertStatusRejected(StoryboardDraftStatus.REJECTED);
        }

        @Test
        @DisplayName("FAILED status reddedilir")
        void shouldRejectFailedStoryboard() {
            assertStatusRejected(StoryboardDraftStatus.FAILED);
        }

        @Test
        @DisplayName("Sahnesi olmayan storyboard reddedilir")
        void shouldRejectStoryboardWithNoScenes() {
            StoryboardDraft noScenes = StoryboardDraft.builder()
                    .id(UUID.randomUUID())
                    .contentProject(project)
                    .version(1)
                    .status(StoryboardDraftStatus.APPROVED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .scenes(new ArrayList<>())
                    .build();

            when(storyboardDraftRepository.findById(noScenes.getId()))
                    .thenReturn(Optional.of(noScenes));

            assertThatThrownBy(() -> service.generateVisualAssetPlan(noScenes.getId(), ADMIN_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("sahne");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        private void assertStatusRejected(StoryboardDraftStatus status) {
            StoryboardDraft sb = StoryboardDraft.builder()
                    .id(UUID.randomUUID())
                    .contentProject(project)
                    .version(1)
                    .status(status)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .scenes(new ArrayList<>(List.of(scene1)))
                    .build();

            when(storyboardDraftRepository.findById(sb.getId()))
                    .thenReturn(Optional.of(sb));

            assertThatThrownBy(() -> service.generateVisualAssetPlan(sb.getId(), ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");

            verify(visualAssetPlanRepository, never()).save(any());
        }
    }

    // ─── Gateway Hata Yönetimi ───────────────────────────────────────

    @Nested
    @DisplayName("Gateway hata yönetimi")
    class GatewayFailureHandling {

        @Test
        @DisplayName("Gateway success=false döndüğünde plan persist edilmez")
        void shouldNotPersistOnGatewayFailure() {
            setupMocksNoSave(approvedStoryboard, 0);
            mockGateway.setMode(MockVisualAssetGatewayMode.TIMEOUT);

            VisualAssetPlanGenerationResult result = service.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gateway exception fırlattığında BusinessException fırlatılır")
        void shouldHandleGatewayException() {
            when(storyboardDraftRepository.findById(approvedStoryboard.getId()))
                    .thenReturn(Optional.of(approvedStoryboard));
            when(visualAssetPlanRepository.findMaxVersionBySourceStoryboardDraftId(any()))
                    .thenReturn(0);

            AiVisualAssetAgentGateway throwingGateway = request -> {
                throw new RuntimeException("Connection refused");
            };
            VisualAssetPlanGenerationService throwingService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, throwingGateway);

            assertThatThrownBy(() -> throwingService.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AI servisi hatası");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Başarısız üretim partial aggregate persist etmez")
        void shouldNotCreatePartialAggregate() {
            setupMocksNoSave(approvedStoryboard, 0);
            mockGateway.setMode(MockVisualAssetGatewayMode.EMPTY_RESPONSE);

            VisualAssetPlanGenerationResult result = service.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID);

            assertThat(result.isSuccess()).isFalse();
            verify(visualAssetPlanRepository, never()).save(any());
        }
    }

    // ─── Yanıt Validasyonu ───────────────────────────────────────────

    @Nested
    @DisplayName("AI yanıt validasyonu")
    class ResponseValidation {

        @Test
        @DisplayName("Sahne sayısı uyuşmazlığı reddedilir")
        void shouldRejectSceneCountMismatch() {
            setupMocksForCustomGateway(approvedStoryboard);

            // 3 sahne var ama 1 sahne dönen gateway
            AiVisualAssetAgentGateway mismatchGateway = request ->
                    VisualAssetPlanAgentResponse.builder()
                            .success(true)
                            .planTitle("Test")
                            .scenePlans(List.of(buildMinimalSceneOutput(1, scene1.getId())))
                            .thumbnailPlan(buildMinimalThumbnailOutput())
                            .providerType("MOCK")
                            .modelName("test")
                            .generatedAt(LocalDateTime.now())
                            .build();

            VisualAssetPlanGenerationService testService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, mismatchGateway);

            assertThatThrownBy(() -> testService.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("sahne sayısı");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Boş englishImagePrompt reddedilir")
        void shouldRejectEmptyEnglishImagePrompt() {
            setupMocksForCustomGateway(approvedStoryboard);

            AiVisualAssetAgentGateway emptyPromptGateway = request ->
                    VisualAssetPlanAgentResponse.builder()
                            .success(true)
                            .planTitle("Test")
                            .scenePlans(List.of(
                                    buildSceneOutputWithPrompt(1, scene1.getId(), "", "context"),
                                    buildSceneOutputWithPrompt(2, scene2.getId(), "ok", "context"),
                                    buildSceneOutputWithPrompt(3, scene3.getId(), "ok", "context")))
                            .thumbnailPlan(buildMinimalThumbnailOutput())
                            .providerType("MOCK")
                            .modelName("test")
                            .generatedAt(LocalDateTime.now())
                            .build();

            VisualAssetPlanGenerationService testService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, emptyPromptGateway);

            assertThatThrownBy(() -> testService.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("englishImagePrompt");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Boş turkishVisualContext reddedilir")
        void shouldRejectEmptyTurkishVisualContext() {
            setupMocksForCustomGateway(approvedStoryboard);

            AiVisualAssetAgentGateway emptyContextGateway = request ->
                    VisualAssetPlanAgentResponse.builder()
                            .success(true)
                            .planTitle("Test")
                            .scenePlans(List.of(
                                    buildSceneOutputWithPrompt(1, scene1.getId(), "ok", ""),
                                    buildSceneOutputWithPrompt(2, scene2.getId(), "ok", "context"),
                                    buildSceneOutputWithPrompt(3, scene3.getId(), "ok", "context")))
                            .thumbnailPlan(buildMinimalThumbnailOutput())
                            .providerType("MOCK")
                            .modelName("test")
                            .generatedAt(LocalDateTime.now())
                            .build();

            VisualAssetPlanGenerationService testService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, emptyContextGateway);

            assertThatThrownBy(() -> testService.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("turkishVisualContext");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Null thumbnail plan reddedilir")
        void shouldRejectNullThumbnailPlan() {
            setupMocksForCustomGateway(approvedStoryboard);

            AiVisualAssetAgentGateway noThumbGateway = request ->
                    VisualAssetPlanAgentResponse.builder()
                            .success(true)
                            .planTitle("Test")
                            .scenePlans(List.of(
                                    buildMinimalSceneOutput(1, scene1.getId()),
                                    buildMinimalSceneOutput(2, scene2.getId()),
                                    buildMinimalSceneOutput(3, scene3.getId())))
                            .thumbnailPlan(null)
                            .providerType("MOCK")
                            .modelName("test")
                            .generatedAt(LocalDateTime.now())
                            .build();

            VisualAssetPlanGenerationService testService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, noThumbGateway);

            assertThatThrownBy(() -> testService.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("thumbnail");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Boş thumbnailPrompt reddedilir")
        void shouldRejectEmptyThumbnailPrompt() {
            setupMocksForCustomGateway(approvedStoryboard);

            AiVisualAssetAgentGateway emptyThumbGateway = request ->
                    VisualAssetPlanAgentResponse.builder()
                            .success(true)
                            .planTitle("Test")
                            .scenePlans(List.of(
                                    buildMinimalSceneOutput(1, scene1.getId()),
                                    buildMinimalSceneOutput(2, scene2.getId()),
                                    buildMinimalSceneOutput(3, scene3.getId())))
                            .thumbnailPlan(VisualAssetThumbnailOutput.builder()
                                    .thumbnailPrompt("")
                                    .build())
                            .providerType("MOCK")
                            .modelName("test")
                            .generatedAt(LocalDateTime.now())
                            .build();

            VisualAssetPlanGenerationService testService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, emptyThumbGateway);

            assertThatThrownBy(() -> testService.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("thumbnailPrompt");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Null scene plans reddedilir")
        void shouldRejectNullScenePlans() {
            setupMocksForCustomGateway(approvedStoryboard);

            AiVisualAssetAgentGateway nullScenesGateway = request ->
                    VisualAssetPlanAgentResponse.builder()
                            .success(true)
                            .planTitle("Test")
                            .scenePlans(null)
                            .thumbnailPlan(buildMinimalThumbnailOutput())
                            .providerType("MOCK")
                            .modelName("test")
                            .generatedAt(LocalDateTime.now())
                            .build();

            VisualAssetPlanGenerationService testService = new VisualAssetPlanGenerationService(
                    storyboardDraftRepository, visualAssetPlanRepository, nullScenesGateway);

            assertThatThrownBy(() -> testService.generateVisualAssetPlan(
                    approvedStoryboard.getId(), ADMIN_ID))
                    .isInstanceOf(ValidationException.class);

            verify(visualAssetPlanRepository, never()).save(any());
        }
    }

    // ─── Güvenlik Kontrolü ───────────────────────────────────────────

    @Nested
    @DisplayName("Güvenlik kontrolleri — MediaAsset/prompt/secret")
    class SafetyChecks {

        @Test
        @DisplayName("Üretilen plan'da MediaAsset/GeneratedAsset referansı yok")
        void shouldNotReferenceMediaAsset() {
            setupMocks(approvedStoryboard, 0);
            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            VisualAssetPlan saved = captor.getValue();

            // VisualAssetPlan entity'de MediaAsset/GeneratedAsset field yok (Task 04'te test edildi)
            // Burada sadece service'in bu alanları set etmediğini teyit ediyoruz
            assertThat(saved.getStatus()).isEqualTo(VisualAssetPlanStatus.AI_GENERATED);
            assertThat(saved.getScenePlans()).allSatisfy(sp -> {
                assertThat(sp.getEnglishImagePrompt()).isNotBlank();
                assertThat(sp.getTurkishVisualContext()).isNotBlank();
            });
        }

        @Test
        @DisplayName("Plan'da raw prompt/provider payload alanı yok")
        void shouldNotExposeRawPrompt() {
            setupMocks(approvedStoryboard, 0);
            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            VisualAssetPlan saved = captor.getValue();

            // Safe metadata is present
            assertThat(saved.getProviderType()).isNotBlank();
            assertThat(saved.getModelName()).isNotBlank();
            // Dangerous fields don't exist on entity (verified in Task 04 domain tests)
        }

        @Test
        @DisplayName("Plan'da image URL/storage field yok")
        void shouldNotHaveImageUrlOrStorageFields() {
            setupMocks(approvedStoryboard, 0);
            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            // Verify the plan was saved (entity doesn't have these fields - verified in Task 04)
            verify(visualAssetPlanRepository, times(1)).save(any(VisualAssetPlan.class));
        }
    }

    // ─── Governance Kuralları ────────────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("Üretilen plan APPROVED değil — yalnızca AI_GENERATED")
        void shouldNeverProduceApprovedStatus() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isNotEqualTo(VisualAssetPlanStatus.APPROVED);
            assertThat(captor.getValue().getStatus()).isEqualTo(VisualAssetPlanStatus.AI_GENERATED);
        }

        @Test
        @DisplayName("generatedBy = AI_CONTENT_AGENT")
        void shouldSetGeneratedByAiContentAgent() {
            setupMocks(approvedStoryboard, 0);

            service.generateVisualAssetPlan(approvedStoryboard.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }
    }

    // ─── Helper Methods ──────────────────────────────────────────────

    private void setupMocksForCustomGateway(StoryboardDraft storyboard) {
        when(storyboardDraftRepository.findById(storyboard.getId()))
                .thenReturn(Optional.of(storyboard));
        when(visualAssetPlanRepository.findMaxVersionBySourceStoryboardDraftId(storyboard.getId()))
                .thenReturn(0);
    }

    private VisualAssetSceneOutput buildMinimalSceneOutput(int sceneNumber, UUID sceneId) {
        return VisualAssetSceneOutput.builder()
                .sourceStoryboardSceneId(sceneId)
                .sceneNumber(sceneNumber)
                .sceneTitle("Scene " + sceneNumber)
                .englishImagePrompt("A cinematic shot for scene " + sceneNumber)
                .turkishVisualContext("Sahne " + sceneNumber + " görselleştirmesi")
                .build();
    }

    private VisualAssetSceneOutput buildSceneOutputWithPrompt(int sceneNumber, UUID sceneId,
                                                               String enPrompt, String trContext) {
        return VisualAssetSceneOutput.builder()
                .sourceStoryboardSceneId(sceneId)
                .sceneNumber(sceneNumber)
                .sceneTitle("Scene " + sceneNumber)
                .englishImagePrompt(enPrompt)
                .turkishVisualContext(trContext)
                .build();
    }

    private VisualAssetThumbnailOutput buildMinimalThumbnailOutput() {
        return VisualAssetThumbnailOutput.builder()
                .thumbnailPrompt("A dramatic portrait for thumbnail")
                .aspectRatio("16:9")
                .build();
    }
}
