package com.anadoluatlasi.backend.application.platformpackage;

import com.anadoluatlasi.backend.ai.gateway.AiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageSceneUsage;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import com.anadoluatlasi.backend.platformpackage.repository.PlatformPackageDraftRepository;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-004: PlatformPackageReviewService unit testleri.
 *
 * <p>Edit, approve, reject akışları, status geçiş kuralları,
 * one-approved-per-platform, governance ve alan güvenliği testleri.</p>
 */
@ExtendWith(MockitoExtension.class)
class PlatformPackageReviewServiceTest {

    @Mock
    private PlatformPackageDraftRepository platformPackageDraftRepository;

    private PlatformPackageReviewService reviewService;

    private ContentProject project;
    private StoryboardDraft storyboard;

    @BeforeEach
    void setUp() {
        reviewService = new PlatformPackageReviewService(platformPackageDraftRepository);

        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.APPROVED)
                .language("tr")
                .retryCount(0)
                .build();

        ContentDraft contentDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Test")
                .body("Body")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        ScenarioDraft scenarioDraft = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(contentDraft)
                .version(1)
                .status(ScenarioDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        storyboard = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(contentDraft)
                .sourceScenarioDraft(scenarioDraft)
                .sourceScenarioVersion(1)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    private PlatformPackageDraft createDraft(PlatformPackageType type, PlatformPackageStatus status) {
        PlatformPackageDraft draft = PlatformPackageDraft.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboard)
                .packageType(type)
                .status(status)
                .version(1)
                .title("Test Paketi")
                .captionOrDescription("Test açıklama")
                .callToAction("Abone ol!")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-v1")
                .build();
        draft.onPrePersist();
        return draft;
    }

    // ── Edit Flow ───────────────────────────────────────────

    @Nested
    @DisplayName("Edit akışı")
    class EditFlow {

        @Test
        @DisplayName("AI_GENERATED → NEEDS_REVIEW düzenleme başarılı")
        void shouldEditAiGeneratedToNeedsReview() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder()
                    .title("Düzenlenmiş Başlık")
                    .build();

            PlatformPackageReviewResult result = reviewService.editPlatformPackageDraft(
                    draft.getId(), cmd, "admin-test");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
            assertThat(draft.getTitle()).isEqualTo("Düzenlenmiş Başlık");
            assertThat(draft.getEditedBy()).isEqualTo("admin-test");
            assertThat(draft.getEditedAt()).isNotNull();
        }

        @Test
        @DisplayName("NEEDS_REVIEW → NEEDS_REVIEW düzenleme başarılı")
        void shouldEditNeedsReviewToNeedsReview() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_SHORTS,
                    PlatformPackageStatus.NEEDS_REVIEW);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder()
                    .captionOrDescription("Yeni açıklama")
                    .shortHook("Yeni hook!")
                    .build();

            PlatformPackageReviewResult result = reviewService.editPlatformPackageDraft(
                    draft.getId(), cmd, "admin-test");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
            assertThat(draft.getCaptionOrDescription()).isEqualTo("Yeni açıklama");
            assertThat(draft.getShortHook()).isEqualTo("Yeni hook!");
        }

        @Test
        @DisplayName("APPROVED düzenleme başarısız")
        void shouldFailEditApproved() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.APPROVED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder().title("X").build();

            assertThatThrownBy(() -> reviewService.editPlatformPackageDraft(draft.getId(), cmd, "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("REJECTED düzenleme başarısız")
        void shouldFailEditRejected() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.REJECTED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder().title("X").build();

            assertThatThrownBy(() -> reviewService.editPlatformPackageDraft(draft.getId(), cmd, "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("REJECTED");
        }

        @Test
        @DisplayName("FAILED düzenleme başarısız")
        void shouldFailEditFailed() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.FAILED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder().title("X").build();

            assertThatThrownBy(() -> reviewService.editPlatformPackageDraft(draft.getId(), cmd, "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("Edit version değiştirmez")
        void shouldNotChangeVersion() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder()
                    .title("Yeni").build();

            reviewService.editPlatformPackageDraft(draft.getId(), cmd, "admin");
            assertThat(draft.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Edit source references değiştirmez")
        void shouldNotChangeSourceReferences() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            UUID originalProjectId = project.getId();
            UUID originalStoryboardId = storyboard.getId();

            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder()
                    .title("Yeni").build();

            reviewService.editPlatformPackageDraft(draft.getId(), cmd, "admin");
            assertThat(draft.getContentProject().getId()).isEqualTo(originalProjectId);
            assertThat(draft.getSourceStoryboardDraft().getId()).isEqualTo(originalStoryboardId);
        }

        @Test
        @DisplayName("Edit provider metadata değiştirmez")
        void shouldNotChangeProviderMetadata() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageDraftEditCommand cmd = PlatformPackageDraftEditCommand.builder()
                    .title("Yeni").build();

            reviewService.editPlatformPackageDraft(draft.getId(), cmd, "admin");
            assertThat(draft.getProviderType()).isEqualTo("MOCK");
            assertThat(draft.getModelName()).isEqualTo("mock-v1");
            assertThat(draft.getGeneratedBy()).isEqualTo(GeneratedBy.AI_CONTENT_AGENT);
        }
    }

    // ── Approve Flow ────────────────────────────────────────

    @Nested
    @DisplayName("Approve akışı")
    class ApproveFlow {

        @Test
        @DisplayName("AI_GENERATED onaylama başarılı")
        void shouldApproveAiGenerated() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.countByProjectAndTypeAndStatus(
                    any(), any(), any())).thenReturn(0L);
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageReviewResult result = reviewService.approvePlatformPackageDraft(
                    draft.getId(), "admin-test");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(draft.getApprovedAt()).isNotNull();
            assertThat(draft.getApprovedBy()).isEqualTo("admin-test");
        }

        @Test
        @DisplayName("NEEDS_REVIEW onaylama başarılı")
        void shouldApproveNeedsReview() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_SHORTS,
                    PlatformPackageStatus.NEEDS_REVIEW);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.countByProjectAndTypeAndStatus(
                    any(), any(), any())).thenReturn(0L);
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageReviewResult result = reviewService.approvePlatformPackageDraft(
                    draft.getId(), "admin-test");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("APPROVED duplicate onaylama başarısız")
        void shouldFailApproveDuplicate() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.APPROVED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.approvePlatformPackageDraft(draft.getId(), "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");

            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECTED onaylama başarısız")
        void shouldFailApproveRejected() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.REJECTED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.approvePlatformPackageDraft(draft.getId(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("FAILED onaylama başarısız")
        void shouldFailApproveFailed() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.FAILED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.approvePlatformPackageDraft(draft.getId(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Onay READY_FOR_PUBLISH ayarlamaz")
        void shouldNotSetReadyForPublish() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.countByProjectAndTypeAndStatus(
                    any(), any(), any())).thenReturn(0L);
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reviewService.approvePlatformPackageDraft(draft.getId(), "admin");

            assertThat(draft.getStatus()).isEqualTo(PlatformPackageStatus.APPROVED);
            assertThat(draft.getStatus().name()).isNotEqualTo("READY_FOR_PUBLISH");
        }
    }

    // ── One-Approved-Per-Platform ────────────────────────────

    @Nested
    @DisplayName("One-approved-per-platform kuralı")
    class OneApprovedPerPlatform {

        @Test
        @DisplayName("İlk YOUTUBE_LONG_FORM onayı başarılı")
        void shouldApproveFirstYoutubeLongForm() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.countByProjectAndTypeAndStatus(
                    project.getId(), PlatformPackageType.YOUTUBE_LONG_FORM, PlatformPackageStatus.APPROVED))
                    .thenReturn(0L);
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageReviewResult result = reviewService.approvePlatformPackageDraft(
                    draft.getId(), "admin");
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("İkinci YOUTUBE_LONG_FORM onayı aynı projede başarısız")
        void shouldFailSecondYoutubeLongFormApproval() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.countByProjectAndTypeAndStatus(
                    project.getId(), PlatformPackageType.YOUTUBE_LONG_FORM, PlatformPackageStatus.APPROVED))
                    .thenReturn(1L);

            assertThatThrownBy(() -> reviewService.approvePlatformPackageDraft(draft.getId(), "admin"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("YOUTUBE_LONG_FORM")
                    .hasMessageContaining("zaten onaylanmış");

            verify(platformPackageDraftRepository, never()).save(any());
        }

        @Test
        @DisplayName("YOUTUBE_SHORTS bağımsız olarak onaylanabilir")
        void shouldApproveShortsIndependently() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_SHORTS,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.countByProjectAndTypeAndStatus(
                    project.getId(), PlatformPackageType.YOUTUBE_SHORTS, PlatformPackageStatus.APPROVED))
                    .thenReturn(0L);
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageReviewResult result = reviewService.approvePlatformPackageDraft(
                    draft.getId(), "admin");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPackageType()).isEqualTo("YOUTUBE_SHORTS");
        }
    }

    // ── Reject Flow ─────────────────────────────────────────

    @Nested
    @DisplayName("Reject akışı")
    class RejectFlow {

        @Test
        @DisplayName("AI_GENERATED reddi başarılı")
        void shouldRejectAiGenerated() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageReviewResult result = reviewService.rejectPlatformPackageDraft(
                    draft.getId(), "admin", "İçerik uygun değil");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("REJECTED");
            assertThat(draft.getRejectedAt()).isNotNull();
            assertThat(draft.getRejectedBy()).isEqualTo("admin");
            assertThat(draft.getRejectionReason()).isEqualTo("İçerik uygun değil");
        }

        @Test
        @DisplayName("NEEDS_REVIEW reddi başarılı")
        void shouldRejectNeedsReview() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.INSTAGRAM_REELS,
                    PlatformPackageStatus.NEEDS_REVIEW);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
            when(platformPackageDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlatformPackageReviewResult result = reviewService.rejectPlatformPackageDraft(
                    draft.getId(), "admin", "Reels formatına uygun değil");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("APPROVED reddi başarısız")
        void shouldFailRejectApproved() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.APPROVED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.rejectPlatformPackageDraft(
                    draft.getId(), "admin", "reason"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("REJECTED duplicate reddi başarısız")
        void shouldFailRejectDuplicate() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.REJECTED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.rejectPlatformPackageDraft(
                    draft.getId(), "admin", "reason"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("REJECTED");
        }

        @Test
        @DisplayName("FAILED reddi başarısız")
        void shouldFailRejectFailed() {
            PlatformPackageDraft draft = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.FAILED);
            when(platformPackageDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> reviewService.rejectPlatformPackageDraft(
                    draft.getId(), "admin", "reason"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Query Service ───────────────────────────────────────

    @Nested
    @DisplayName("Query service")
    class QueryServiceTests {

        private PlatformPackageQueryService queryService;

        @BeforeEach
        void setupQueryService() {
            queryService = new PlatformPackageQueryService(platformPackageDraftRepository);
        }

        @Test
        @DisplayName("listByContentProject sonuç döner")
        void shouldListByContentProject() {
            PlatformPackageDraft d1 = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            PlatformPackageDraft d2 = createDraft(PlatformPackageType.YOUTUBE_SHORTS,
                    PlatformPackageStatus.AI_GENERATED);

            when(platformPackageDraftRepository.findByContentProjectIdOrderByPackageTypeAscVersionDesc(
                    project.getId())).thenReturn(List.of(d1, d2));

            List<PlatformPackageDraft> results = queryService.listByContentProject(project.getId());
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("listBySourceStoryboard sonuç döner")
        void shouldListBySourceStoryboard() {
            PlatformPackageDraft d1 = createDraft(PlatformPackageType.INSTAGRAM_REELS,
                    PlatformPackageStatus.AI_GENERATED);

            when(platformPackageDraftRepository.findBySourceStoryboardDraftIdOrderByPackageTypeAscVersionDesc(
                    storyboard.getId())).thenReturn(List.of(d1));

            List<PlatformPackageDraft> results = queryService.listBySourceStoryboard(storyboard.getId());
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("getDetail scene usages ile döner")
        void shouldGetDetailWithSceneUsages() {
            PlatformPackageDraft d = createDraft(PlatformPackageType.YOUTUBE_LONG_FORM,
                    PlatformPackageStatus.AI_GENERATED);
            PlatformPackageSceneUsage usage = PlatformPackageSceneUsage.builder()
                    .platformPackageDraft(d)
                    .sceneNumber(1)
                    .usageType("PRIMARY")
                    .included(true)
                    .build();
            usage.onPrePersist();
            d.getSceneUsages().add(usage);

            when(platformPackageDraftRepository.findById(d.getId())).thenReturn(Optional.of(d));

            PlatformPackageDraft detail = queryService.getDetail(d.getId());
            assertThat(detail.getSceneUsages()).hasSize(1);
            assertThat(detail.getSceneUsages().get(0).getUsageType()).isEqualTo("PRIMARY");
        }

        @Test
        @DisplayName("getDetail olmayan ID → NotFoundException")
        void shouldThrowOnMissingDetail() {
            UUID missing = UUID.randomUUID();
            when(platformPackageDraftRepository.findById(missing)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.getDetail(missing))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── Governance ──────────────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("READY_FOR_PUBLISH status mevcut değil")
        void shouldNotHaveReadyForPublish() {
            for (PlatformPackageStatus status : PlatformPackageStatus.values()) {
                assertThat(status.name()).isNotEqualTo("READY_FOR_PUBLISH");
            }
        }

        @Test
        @DisplayName("ReviewService AI gateway dependency yok")
        void shouldNotDependOnAiGateway() {
            java.lang.reflect.Field[] fields = PlatformPackageReviewService.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                assertThat(field.getType()).isNotEqualTo(AiPlatformPackageAgentGateway.class);
            }
        }

        @Test
        @DisplayName("Generic status update metodu yok")
        void shouldNotHaveGenericStatusUpdateMethod() {
            java.lang.reflect.Method[] methods = PlatformPackageReviewService.class.getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                String name = method.getName().toLowerCase();
                assertThat(name).doesNotContain("updatestatus");
                assertThat(name).doesNotContain("setstatus");
                assertThat(name).doesNotContain("changestatus");
            }
        }

        @Test
        @DisplayName("Entity'de rawPrompt/apiKey/secret alanı yok")
        void shouldNotHaveUnsafeFields() {
            java.lang.reflect.Field[] fields = PlatformPackageDraft.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("rawprompt");
                assertThat(name).doesNotContain("apikey");
                assertThat(name).doesNotContain("secret");
            }
        }
    }
}
