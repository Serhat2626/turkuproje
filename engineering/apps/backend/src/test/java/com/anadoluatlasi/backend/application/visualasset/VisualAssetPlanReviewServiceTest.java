package com.anadoluatlasi.backend.application.visualasset;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.visualasset.entity.SceneVisualPlan;
import com.anadoluatlasi.backend.visualasset.entity.ThumbnailPlan;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-006: VisualAssetPlanReviewService unit testleri.
 */
@ExtendWith(MockitoExtension.class)
class VisualAssetPlanReviewServiceTest {

    @Mock
    private VisualAssetPlanRepository visualAssetPlanRepository;

    private VisualAssetPlanReviewService service;

    private ContentProject project;
    private StoryboardDraft storyboard;
    private VisualAssetPlan plan;
    private SceneVisualPlan scenePlan1;
    private SceneVisualPlan scenePlan2;
    private ThumbnailPlan thumbnailPlan;

    private static final String ADMIN_ID = "admin-user-1";

    @BeforeEach
    void setUp() {
        service = new VisualAssetPlanReviewService(visualAssetPlanRepository);

        project = ContentProject.builder().id(UUID.randomUUID()).build();
        storyboard = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .version(1)
                .status(StoryboardDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .scenes(new ArrayList<>())
                .build();

        scenePlan1 = SceneVisualPlan.builder()
                .id(UUID.randomUUID())
                .sceneNumber(1)
                .sceneTitle("Sahne 1")
                .englishImagePrompt("A cinematic shot")
                .turkishVisualContext("Sinematik çekim")
                .build();

        scenePlan2 = SceneVisualPlan.builder()
                .id(UUID.randomUUID())
                .sceneNumber(2)
                .sceneTitle("Sahne 2")
                .englishImagePrompt("A dramatic view")
                .turkishVisualContext("Dramatik görüntü")
                .build();

        thumbnailPlan = ThumbnailPlan.builder()
                .id(UUID.randomUUID())
                .thumbnailPrompt("A dramatic portrait")
                .aspectRatio("16:9")
                .build();

        plan = VisualAssetPlan.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceStoryboardDraft(storyboard)
                .version(1)
                .status(VisualAssetPlanStatus.AI_GENERATED)
                .title("Test Plan")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-visual-asset-planner")
                .scenePlans(new ArrayList<>(List.of(scenePlan1, scenePlan2)))
                .thumbnailPlan(thumbnailPlan)
                .build();
    }

    private void setupFindById(VisualAssetPlan p) {
        when(visualAssetPlanRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(visualAssetPlanRepository.save(any(VisualAssetPlan.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupFindByIdNoSave(VisualAssetPlan p) {
        when(visualAssetPlanRepository.findById(p.getId())).thenReturn(Optional.of(p));
    }

    // ─── Edit Parent Plan ────────────────────────────────────────────

    @Nested
    @DisplayName("Parent plan düzenleme")
    class EditParentPlan {

        @Test
        @DisplayName("Parent alanları güncellenir ve status NEEDS_REVIEW olur")
        void shouldUpdateFieldsAndSetNeedsReview() {
            setupFindById(plan);

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("Güncellenmiş Başlık")
                    .overallVisualStyle("New style")
                    .build();

            VisualAssetPlanReviewResult result = service.updatePlan(plan.getId(), cmd, ADMIN_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            VisualAssetPlan saved = captor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Güncellenmiş Başlık");
            assertThat(saved.getOverallVisualStyle()).isEqualTo("New style");
            assertThat(saved.getStatus()).isEqualTo(VisualAssetPlanStatus.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("Edit editedAt ve editedBy günceller")
        void shouldSetEditedMetadata() {
            setupFindById(plan);

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("Updated").build();

            service.updatePlan(plan.getId(), cmd, ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getEditedAt()).isNotNull();
            assertThat(captor.getValue().getEditedBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("APPROVED plan düzenlendiğinde NEEDS_REVIEW'a geçer")
        void shouldTransitionApprovedToNeedsReview() {
            plan.setStatus(VisualAssetPlanStatus.APPROVED);
            setupFindById(plan);

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("Changed").build();

            VisualAssetPlanReviewResult result = service.updatePlan(plan.getId(), cmd, ADMIN_ID);
            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("REJECTED plan düzenlendiğinde NEEDS_REVIEW'a geçer")
        void shouldTransitionRejectedToNeedsReview() {
            plan.setStatus(VisualAssetPlanStatus.REJECTED);
            setupFindById(plan);

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("Fixed").build();

            VisualAssetPlanReviewResult result = service.updatePlan(plan.getId(), cmd, ADMIN_ID);
            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("FAILED plan düzenlenemez")
        void shouldRejectFailedEdit() {
            plan.setStatus(VisualAssetPlanStatus.FAILED);
            setupFindByIdNoSave(plan);

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("Fix").build();

            assertThatThrownBy(() -> service.updatePlan(plan.getId(), cmd, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("düzenlenemez");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Edit asla APPROVED yapmaz")
        void shouldNeverApproveViaEdit() {
            setupFindById(plan);

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("Updated").build();

            service.updatePlan(plan.getId(), cmd, ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isNotEqualTo(VisualAssetPlanStatus.APPROVED);
        }

        @Test
        @DisplayName("Edit asla READY_FOR_PUBLISH oluşturmaz")
        void shouldNotCreateReadyForPublish() {
            setupFindById(plan);

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("Updated").build();

            VisualAssetPlanReviewResult result = service.updatePlan(plan.getId(), cmd, ADMIN_ID);
            assertThat(result.getStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }
    }

    // ─── Edit Scene Plan ─────────────────────────────────────────────

    @Nested
    @DisplayName("Scene plan düzenleme")
    class EditScenePlan {

        @Test
        @DisplayName("Scene düzenleme parent status'ü NEEDS_REVIEW yapar")
        void shouldSetParentNeedsReview() {
            setupFindById(plan);

            UpdateSceneVisualPlanCommand cmd = UpdateSceneVisualPlanCommand.builder()
                    .englishImagePrompt("Updated prompt")
                    .build();

            VisualAssetPlanReviewResult result = service.updateScenePlan(
                    plan.getId(), 1, cmd, ADMIN_ID);

            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("englishImagePrompt güncellenir")
        void shouldUpdateEnglishImagePrompt() {
            setupFindById(plan);

            UpdateSceneVisualPlanCommand cmd = UpdateSceneVisualPlanCommand.builder()
                    .englishImagePrompt("A beautiful sunrise over Anatolian village")
                    .build();

            service.updateScenePlan(plan.getId(), 1, cmd, ADMIN_ID);

            assertThat(scenePlan1.getEnglishImagePrompt())
                    .isEqualTo("A beautiful sunrise over Anatolian village");
        }

        @Test
        @DisplayName("turkishVisualContext güncellenir")
        void shouldUpdateTurkishVisualContext() {
            setupFindById(plan);

            UpdateSceneVisualPlanCommand cmd = UpdateSceneVisualPlanCommand.builder()
                    .turkishVisualContext("Anadolu köyünde gündoğumu")
                    .build();

            service.updateScenePlan(plan.getId(), 1, cmd, ADMIN_ID);

            assertThat(scenePlan1.getTurkishVisualContext())
                    .isEqualTo("Anadolu köyünde gündoğumu");
        }

        @Test
        @DisplayName("Olmayan sceneNumber NotFoundException fırlatır")
        void shouldThrowForMissingScene() {
            setupFindByIdNoSave(plan);

            UpdateSceneVisualPlanCommand cmd = UpdateSceneVisualPlanCommand.builder()
                    .englishImagePrompt("test").build();

            assertThatThrownBy(() -> service.updateScenePlan(plan.getId(), 99, cmd, ADMIN_ID))
                    .isInstanceOf(NotFoundException.class);

            verify(visualAssetPlanRepository, never()).save(any());
        }
    }

    // ─── Edit Thumbnail Plan ─────────────────────────────────────────

    @Nested
    @DisplayName("Thumbnail plan düzenleme")
    class EditThumbnailPlan {

        @Test
        @DisplayName("Thumbnail prompt güncellenir")
        void shouldUpdateThumbnailPrompt() {
            setupFindById(plan);

            UpdateThumbnailPlanCommand cmd = UpdateThumbnailPlanCommand.builder()
                    .thumbnailPrompt("Updated thumbnail prompt")
                    .build();

            service.updateThumbnailPlan(plan.getId(), cmd, ADMIN_ID);

            assertThat(thumbnailPlan.getThumbnailPrompt()).isEqualTo("Updated thumbnail prompt");
        }

        @Test
        @DisplayName("Thumbnail düzenleme parent status'ü NEEDS_REVIEW yapar")
        void shouldSetParentNeedsReview() {
            setupFindById(plan);

            UpdateThumbnailPlanCommand cmd = UpdateThumbnailPlanCommand.builder()
                    .thumbnailPrompt("New prompt").build();

            VisualAssetPlanReviewResult result = service.updateThumbnailPlan(
                    plan.getId(), cmd, ADMIN_ID);

            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("ThumbnailPlan yoksa NotFoundException fırlatır")
        void shouldThrowIfNoThumbnail() {
            plan.setThumbnailPlan(null);
            setupFindByIdNoSave(plan);

            UpdateThumbnailPlanCommand cmd = UpdateThumbnailPlanCommand.builder()
                    .thumbnailPrompt("test").build();

            assertThatThrownBy(() -> service.updateThumbnailPlan(plan.getId(), cmd, ADMIN_ID))
                    .isInstanceOf(NotFoundException.class);

            verify(visualAssetPlanRepository, never()).save(any());
        }
    }

    // ─── Approve ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Onay (approve)")
    class ApproveTests {

        @Test
        @DisplayName("AI_GENERATED → APPROVED")
        void shouldApproveFromAiGenerated() {
            plan.setStatus(VisualAssetPlanStatus.AI_GENERATED);
            setupFindById(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(false);

            VisualAssetPlanReviewResult result = service.approve(plan.getId(), ADMIN_ID);

            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("NEEDS_REVIEW → APPROVED")
        void shouldApproveFromNeedsReview() {
            plan.setStatus(VisualAssetPlanStatus.NEEDS_REVIEW);
            setupFindById(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(false);

            VisualAssetPlanReviewResult result = service.approve(plan.getId(), ADMIN_ID);

            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Approve approvedAt/approvedBy günceller")
        void shouldSetApprovedMetadata() {
            setupFindById(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(false);

            service.approve(plan.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getApprovedAt()).isNotNull();
            assertThat(captor.getValue().getApprovedBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("Approve önceki rejection bilgisini temizler")
        void shouldClearRejectionInfo() {
            plan.setRejectedAt(LocalDateTime.now().minusDays(1));
            plan.setRejectedBy("other-admin");
            plan.setRejectionReason("Not good enough");
            setupFindById(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(false);

            service.approve(plan.getId(), ADMIN_ID);

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getRejectedAt()).isNull();
            assertThat(captor.getValue().getRejectedBy()).isNull();
            assertThat(captor.getValue().getRejectionReason()).isNull();
        }

        @Test
        @DisplayName("REJECTED doğrudan onaylanamaz")
        void shouldRejectApproveFromRejected() {
            plan.setStatus(VisualAssetPlanStatus.REJECTED);
            setupFindByIdNoSave(plan);

            assertThatThrownBy(() -> service.approve(plan.getId(), ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("onaylanamaz");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("FAILED onaylanamaz")
        void shouldRejectApproveFromFailed() {
            plan.setStatus(VisualAssetPlanStatus.FAILED);
            setupFindByIdNoSave(plan);

            assertThatThrownBy(() -> service.approve(plan.getId(), ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("onaylanamaz");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("One-approved-per-storyboard ihlali reddedilir")
        void shouldEnforceOneApprovedPerStoryboard() {
            setupFindByIdNoSave(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(true);

            assertThatThrownBy(() -> service.approve(plan.getId(), ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("zaten onaylanmış");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("One-approved ihlali hedef planı mutate etmez")
        void shouldNotMutatePlanOnViolation() {
            VisualAssetPlanStatus originalStatus = plan.getStatus();
            setupFindByIdNoSave(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(true);

            assertThatThrownBy(() -> service.approve(plan.getId(), ADMIN_ID))
                    .isInstanceOf(BusinessException.class);

            assertThat(plan.getStatus()).isEqualTo(originalStatus);
        }

        @Test
        @DisplayName("Approve asla READY_FOR_PUBLISH oluşturmaz")
        void shouldNotCreateReadyForPublish() {
            setupFindById(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(false);

            VisualAssetPlanReviewResult result = service.approve(plan.getId(), ADMIN_ID);
            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(result.getStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("Approve asla image/media üretmez")
        void shouldNotGenerateMedia() {
            setupFindById(plan);
            when(visualAssetPlanRepository.existsBySourceStoryboardDraftIdAndStatus(
                    storyboard.getId(), VisualAssetPlanStatus.APPROVED)).thenReturn(false);

            service.approve(plan.getId(), ADMIN_ID);

            // Service has no AI gateway or media dependency — verified by constructor
            // No additional interactions beyond findById + existsBy + save
            verify(visualAssetPlanRepository).save(any());
        }
    }

    // ─── Reject ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Red (reject)")
    class RejectTests {

        @Test
        @DisplayName("AI_GENERATED → REJECTED")
        void shouldRejectFromAiGenerated() {
            plan.setStatus(VisualAssetPlanStatus.AI_GENERATED);
            setupFindById(plan);

            VisualAssetPlanReviewResult result = service.reject(
                    plan.getId(), ADMIN_ID, "Kalite yetersiz");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("NEEDS_REVIEW → REJECTED")
        void shouldRejectFromNeedsReview() {
            plan.setStatus(VisualAssetPlanStatus.NEEDS_REVIEW);
            setupFindById(plan);

            VisualAssetPlanReviewResult result = service.reject(
                    plan.getId(), ADMIN_ID, "Revizyon gerekli");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Reject rejectedAt/rejectedBy/rejectionReason günceller")
        void shouldSetRejectedMetadata() {
            setupFindById(plan);

            service.reject(plan.getId(), ADMIN_ID, "Kalite yetersiz");

            ArgumentCaptor<VisualAssetPlan> captor = ArgumentCaptor.forClass(VisualAssetPlan.class);
            verify(visualAssetPlanRepository).save(captor.capture());
            assertThat(captor.getValue().getRejectedAt()).isNotNull();
            assertThat(captor.getValue().getRejectedBy()).isEqualTo(ADMIN_ID);
            assertThat(captor.getValue().getRejectionReason()).isEqualTo("Kalite yetersiz");
        }

        @Test
        @DisplayName("APPROVED doğrudan reddedilemez")
        void shouldRejectRejectFromApproved() {
            plan.setStatus(VisualAssetPlanStatus.APPROVED);
            setupFindByIdNoSave(plan);

            assertThatThrownBy(() -> service.reject(plan.getId(), ADMIN_ID, "reason"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("reddedilemez");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("FAILED reddedilemez")
        void shouldRejectRejectFromFailed() {
            plan.setStatus(VisualAssetPlanStatus.FAILED);
            setupFindByIdNoSave(plan);

            assertThatThrownBy(() -> service.reject(plan.getId(), ADMIN_ID, "reason"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("reddedilemez");

            verify(visualAssetPlanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reject asla READY_FOR_PUBLISH oluşturmaz")
        void shouldNotCreateReadyForPublish() {
            setupFindById(plan);

            VisualAssetPlanReviewResult result = service.reject(
                    plan.getId(), ADMIN_ID, "not good");

            assertThat(result.getStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("Reject asla image/media üretmez")
        void shouldNotGenerateMedia() {
            setupFindById(plan);

            service.reject(plan.getId(), ADMIN_ID, "reason");

            verify(visualAssetPlanRepository).save(any());
            verifyNoMoreInteractions(visualAssetPlanRepository);
        }

        @Test
        @DisplayName("REJECTED tekrar reddedilemez")
        void shouldRejectDoubleReject() {
            plan.setStatus(VisualAssetPlanStatus.REJECTED);
            setupFindByIdNoSave(plan);

            assertThatThrownBy(() -> service.reject(plan.getId(), ADMIN_ID, "reason"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("reddedilemez");

            verify(visualAssetPlanRepository, never()).save(any());
        }
    }

    // ─── Not Found ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Plan bulunamadı")
    class NotFoundTests {

        @Test
        @DisplayName("Olmayan plan ID ile updatePlan NotFoundException fırlatır")
        void shouldThrowNotFoundForUpdatePlan() {
            UUID missingId = UUID.randomUUID();
            when(visualAssetPlanRepository.findById(missingId)).thenReturn(Optional.empty());

            UpdateVisualAssetPlanCommand cmd = UpdateVisualAssetPlanCommand.builder()
                    .title("test").build();

            assertThatThrownBy(() -> service.updatePlan(missingId, cmd, ADMIN_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Olmayan plan ID ile approve NotFoundException fırlatır")
        void shouldThrowNotFoundForApprove() {
            UUID missingId = UUID.randomUUID();
            when(visualAssetPlanRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approve(missingId, ADMIN_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Olmayan plan ID ile reject NotFoundException fırlatır")
        void shouldThrowNotFoundForReject() {
            UUID missingId = UUID.randomUUID();
            when(visualAssetPlanRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reject(missingId, ADMIN_ID, "reason"))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
