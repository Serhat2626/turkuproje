package com.anadoluatlasi.backend.application.storyboard;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
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
 * FP-003: StoryboardReviewService unit testleri.
 *
 * <p>Kritik kurallar:</p>
 * <ul>
 *   <li>Edit/approve/reject hiçbir zaman AI gateway çağırmaz</li>
 *   <li>Approve explicit admin eylemidir — otomatik onay yok</li>
 *   <li>Bir proje için yalnızca bir APPROVED storyboard olabilir</li>
 *   <li>APPROVED/REJECTED/FAILED storyboard'lar düzenlenemez</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class StoryboardReviewServiceTest {

    @Mock
    private StoryboardDraftRepository storyboardDraftRepository;

    private StoryboardReviewService service;

    private ContentProject project;
    private ContentDraft approvedDraft;
    private ScenarioDraft approvedScenario;

    @BeforeEach
    void setUp() {
        service = new StoryboardReviewService(storyboardDraftRepository);

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
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        approvedScenario = ScenarioDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .version(1)
                .status(ScenarioDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    // ── Query: Get Detail ───────────────────────────────────

    @Nested
    @DisplayName("Storyboard detail query")
    class GetDetail {

        @Test
        @DisplayName("1. Mevcut storyboard detayı döndürülür")
        void shouldReturnStoryboardDetail() {
            StoryboardDraft draft = createAiGeneratedDraft();
            StoryboardQueryService queryService = new StoryboardQueryService(storyboardDraftRepository);
            when(storyboardDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

            StoryboardDraft result = queryService.getDetail(draft.getId());

            assertThat(result.getId()).isEqualTo(draft.getId());
            assertThat(result.getTitle()).isEqualTo(draft.getTitle());
        }

        @Test
        @DisplayName("2. Mevcut olmayan storyboard → NotFoundException")
        void shouldThrowNotFoundForMissing() {
            UUID missingId = UUID.randomUUID();
            StoryboardQueryService queryService = new StoryboardQueryService(storyboardDraftRepository);
            when(storyboardDraftRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryService.getDetail(missingId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── Query: List by Project ──────────────────────────────

    @Nested
    @DisplayName("Storyboard list by content project")
    class ListByContentProject {

        @Test
        @DisplayName("3. Content Project'e ait storyboard'lar listelenir")
        void shouldListByContentProject() {
            StoryboardQueryService queryService = new StoryboardQueryService(storyboardDraftRepository);
            StoryboardDraft draft = createAiGeneratedDraft();
            when(storyboardDraftRepository.findByContentProjectIdOrderByVersionDesc(project.getId()))
                    .thenReturn(List.of(draft));

            List<StoryboardDraft> results = queryService.listByContentProject(project.getId());

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(draft.getId());
        }
    }

    // ── Edit ────────────────────────────────────────────────

    @Nested
    @DisplayName("Storyboard edit")
    class Edit {

        @Test
        @DisplayName("4. AI_GENERATED storyboard edit → NEEDS_REVIEW")
        void shouldEditAiGeneratedToNeedsReview() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMock(draft);

            StoryboardDraftEditCommand command = StoryboardDraftEditCommand.builder()
                    .title("Düzenlenmiş Storyboard")
                    .build();

            StoryboardReviewResult result = service.editStoryboardDraft(
                    draft.getId(), command, "admin-serhat");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("5. NEEDS_REVIEW storyboard edit → NEEDS_REVIEW kalır")
        void shouldRemainNeedsReviewAfterEdit() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.NEEDS_REVIEW);
            setupMock(draft);

            StoryboardDraftEditCommand command = StoryboardDraftEditCommand.builder()
                    .summary("Güncellenmiş özet")
                    .build();

            StoryboardReviewResult result = service.editStoryboardDraft(
                    draft.getId(), command, "admin-serhat");

            assertThat(result.getStatus()).isEqualTo("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("6. APPROVED storyboard edit başarısız")
        void shouldFailEditForApproved() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.APPROVED);
            setupMock(draft);

            StoryboardDraftEditCommand command = StoryboardDraftEditCommand.builder()
                    .title("Değişiklik")
                    .build();

            assertThatThrownBy(() -> service.editStoryboardDraft(
                    draft.getId(), command, "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("7. REJECTED storyboard edit başarısız")
        void shouldFailEditForRejected() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.REJECTED);
            setupMock(draft);

            assertThatThrownBy(() -> service.editStoryboardDraft(
                    draft.getId(), StoryboardDraftEditCommand.builder().build(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("8. FAILED storyboard edit başarısız")
        void shouldFailEditForFailed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.FAILED);
            setupMock(draft);

            assertThatThrownBy(() -> service.editStoryboardDraft(
                    draft.getId(), StoryboardDraftEditCommand.builder().build(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("9. Scene edit — içerik güncellenir")
        void shouldApplySceneEdits() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMock(draft);

            StoryboardSceneEditCommand sceneCmd = StoryboardSceneEditCommand.builder()
                    .sceneNumber(1)
                    .visualDescription("Düzenlenmiş görsel açıklama")
                    .narrationText("Düzenlenmiş narration")
                    .cameraDirection("Close-up, yavaş zoom")
                    .mood("Heyecanlı")
                    .build();

            StoryboardDraftEditCommand command = StoryboardDraftEditCommand.builder()
                    .scenes(List.of(sceneCmd))
                    .build();

            StoryboardReviewResult result = service.editStoryboardDraft(
                    draft.getId(), command, "admin-serhat");

            assertThat(result.isSuccess()).isTrue();

            // Scene alanları güncellenmiş olmalı
            StoryboardScene scene = draft.getScenes().get(0);
            assertThat(scene.getVisualDescription()).isEqualTo("Düzenlenmiş görsel açıklama");
            assertThat(scene.getNarrationText()).isEqualTo("Düzenlenmiş narration");
            assertThat(scene.getCameraDirection()).isEqualTo("Close-up, yavaş zoom");
            assertThat(scene.getMood()).isEqualTo("Heyecanlı");
        }

        @Test
        @DisplayName("10. editedAt ve editedBy güncellenir")
        void shouldSetEditAuditFields() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMock(draft);

            StoryboardDraftEditCommand command = StoryboardDraftEditCommand.builder()
                    .title("Audit test")
                    .build();

            service.editStoryboardDraft(draft.getId(), command, "admin-serhat");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository).save(captor.capture());
            StoryboardDraft saved = captor.getValue();

            assertThat(saved.getEditedAt()).isNotNull();
            assertThat(saved.getEditedBy()).isEqualTo("admin-serhat");
        }

        @Test
        @DisplayName("11. Mevcut olmayan scene numarası → sessizce geçilir")
        void shouldSkipNonExistentSceneNumber() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMock(draft);

            StoryboardSceneEditCommand sceneCmd = StoryboardSceneEditCommand.builder()
                    .sceneNumber(99) // mevcut değil
                    .visualDescription("Bu uygulanmamalı")
                    .build();

            StoryboardDraftEditCommand command = StoryboardDraftEditCommand.builder()
                    .scenes(List.of(sceneCmd))
                    .build();

            // Exception fırlatmamalı
            StoryboardReviewResult result = service.editStoryboardDraft(
                    draft.getId(), command, "admin");
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ── Approve ─────────────────────────────────────────────

    @Nested
    @DisplayName("Storyboard approve")
    class Approve {

        @Test
        @DisplayName("12. AI_GENERATED storyboard approve → APPROVED")
        void shouldApproveAiGenerated() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMockForApproval(draft);

            StoryboardReviewResult result = service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("13. NEEDS_REVIEW storyboard approve → APPROVED")
        void shouldApproveNeedsReview() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.NEEDS_REVIEW);
            addValidScenesToDraft(draft);
            setupMockForApproval(draft);

            StoryboardReviewResult result = service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("14. FAILED storyboard approve başarısız")
        void shouldFailApproveForFailed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.FAILED);
            setupMock(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("15. REJECTED storyboard approve başarısız")
        void shouldFailApproveForRejected() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.REJECTED);
            setupMock(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("16. Zaten APPROVED storyboard tekrar approve başarısız")
        void shouldFailDuplicateApprove() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.APPROVED);
            setupMock(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("17. approvedAt ve approvedBy güncellenir")
        void shouldSetApproveAuditFields() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMockForApproval(draft);

            service.approveStoryboardDraft(draft.getId(), "admin-serhat");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository).save(captor.capture());
            StoryboardDraft saved = captor.getValue();

            assertThat(saved.getApprovedAt()).isNotNull();
            assertThat(saved.getApprovedBy()).isEqualTo("admin-serhat");
        }

        @Test
        @DisplayName("18. Başlıksız storyboard approve → validation fail")
        void shouldFailApproveWithoutTitle() {
            StoryboardDraft draft = createAiGeneratedDraft();
            draft.setTitle(null); // başlık yok
            setupMockForApproval(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("TITLE_REQUIRED");
        }

        @Test
        @DisplayName("19. Scene'siz storyboard approve → validation fail")
        void shouldFailApproveWithoutScenes() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.AI_GENERATED);
            // scene eklemeden bırak — boş scene listesi
            setupMockForApproval(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("SCENES_REQUIRED");
        }

        @Test
        @DisplayName("20. visualDescription eksik scene → validation fail")
        void shouldFailApproveWithMissingVisualDescription() {
            StoryboardDraft draft = createAiGeneratedDraft();
            draft.getScenes().get(0).setVisualDescription(null); // eksik
            setupMockForApproval(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("EMPTY_VISUAL_DESCRIPTION");
        }
    }

    // ── Reject ──────────────────────────────────────────────

    @Nested
    @DisplayName("Storyboard reject")
    class Reject {

        @Test
        @DisplayName("21. AI_GENERATED storyboard reject → REJECTED")
        void shouldRejectAiGenerated() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMock(draft);

            StoryboardReviewResult result = service.rejectStoryboardDraft(
                    draft.getId(), "admin-serhat", "Görsel açıklamalar yetersiz");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("22. NEEDS_REVIEW storyboard reject → REJECTED")
        void shouldRejectNeedsReview() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.NEEDS_REVIEW);
            setupMock(draft);

            StoryboardReviewResult result = service.rejectStoryboardDraft(
                    draft.getId(), "admin-serhat", "Yeniden üretilmeli");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("23. APPROVED storyboard reject başarısız")
        void shouldFailRejectForApproved() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.APPROVED);
            setupMock(draft);

            assertThatThrownBy(() -> service.rejectStoryboardDraft(
                    draft.getId(), "admin-serhat", "Red sebebi"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("24. FAILED storyboard reject başarısız")
        void shouldFailRejectForFailed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.FAILED);
            setupMock(draft);

            assertThatThrownBy(() -> service.rejectStoryboardDraft(
                    draft.getId(), "admin-serhat", "Red sebebi"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("25. rejectedAt/rejectedBy/reviewNotes güncellenir")
        void shouldSetRejectAuditFields() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMock(draft);

            service.rejectStoryboardDraft(draft.getId(), "admin-serhat", "Kalite yetersiz");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository).save(captor.capture());
            StoryboardDraft saved = captor.getValue();

            assertThat(saved.getRejectedAt()).isNotNull();
            assertThat(saved.getRejectedBy()).isEqualTo("admin-serhat");
            assertThat(saved.getReviewNotes()).isEqualTo("Kalite yetersiz");
        }
    }

    // ── One-Approved-Storyboard Rule ────────────────────────

    @Nested
    @DisplayName("One-approved-storyboard rule")
    class OneApprovedRule {

        @Test
        @DisplayName("26. İlk storyboard approve başarılı")
        void shouldApproveFirstStoryboard() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMockForApproval(draft);

            StoryboardReviewResult result = service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("27. Aynı proje için ikinci storyboard approve başarısız")
        void shouldFailSecondApprovalForSameProject() {
            // İlk approved storyboard mevcut
            StoryboardDraft existingApproved = createDraftWithStatus(StoryboardDraftStatus.APPROVED);
            existingApproved.setVersion(1);

            // İkinci storyboard approve edilmeye çalışılıyor
            StoryboardDraft secondDraft = createAiGeneratedDraft();
            secondDraft.setVersion(2);

            when(storyboardDraftRepository.findById(secondDraft.getId()))
                    .thenReturn(Optional.of(secondDraft));
            when(storyboardDraftRepository.findByContentProjectIdAndStatus(
                    project.getId(), StoryboardDraftStatus.APPROVED))
                    .thenReturn(List.of(existingApproved));

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    secondDraft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("zaten onaylanmış");
        }

        @Test
        @DisplayName("28. Eski approved storyboard otomatik unapprove edilmez")
        void shouldNotAutoUnapproveOldStoryboard() {
            StoryboardDraft existingApproved = createDraftWithStatus(StoryboardDraftStatus.APPROVED);
            existingApproved.setVersion(1);

            StoryboardDraft secondDraft = createAiGeneratedDraft();
            secondDraft.setVersion(2);

            when(storyboardDraftRepository.findById(secondDraft.getId()))
                    .thenReturn(Optional.of(secondDraft));
            when(storyboardDraftRepository.findByContentProjectIdAndStatus(
                    project.getId(), StoryboardDraftStatus.APPROVED))
                    .thenReturn(List.of(existingApproved));

            assertThatThrownBy(() -> service.approveStoryboardDraft(
                    secondDraft.getId(), "admin-serhat"))
                    .isInstanceOf(BusinessException.class);

            // Eski approved storyboard'ın status'ü değişmemiş olmalı
            assertThat(existingApproved.getStatus()).isEqualTo(StoryboardDraftStatus.APPROVED);
        }
    }

    // ── Governance ──────────────────────────────────────────

    @Nested
    @DisplayName("Governance kuralları")
    class GovernanceRules {

        @Test
        @DisplayName("29. Edit/approve/reject asla AI gateway çağırmaz")
        void shouldNeverCallAiGateway() {
            // Service constructor'ında AI gateway yok — bu zaten mimari olarak garanti
            // Ama review service'te gateway field'ı olmamasını doğrulayalım
            java.lang.reflect.Field[] fields = StoryboardReviewService.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                assertThat(field.getType().getName()).doesNotContain("AiStoryboardAgentGateway");
            }
        }

        @Test
        @DisplayName("30. Approve asla READY_FOR_PUBLISH ayarlamaz")
        void shouldNeverSetReadyForPublish() {
            StoryboardDraft draft = createAiGeneratedDraft();
            setupMockForApproval(draft);

            StoryboardReviewResult result = service.approveStoryboardDraft(
                    draft.getId(), "admin-serhat");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(result.getStatus()).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("31. Raw prompt/payload/secret alanı eklenmemiş")
        void shouldNotIntroduceSecretFields() {
            java.lang.reflect.Field[] fields = StoryboardReviewService.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("prompt");
                assertThat(name).doesNotContain("apikey");
                assertThat(name).doesNotContain("secret");
                assertThat(name).doesNotContain("payload");
            }
        }

        @Test
        @DisplayName("32. Edit yeni version oluşturmaz")
        void editShouldNotCreateNewVersion() {
            StoryboardDraft draft = createAiGeneratedDraft();
            draft.setVersion(1);
            setupMock(draft);

            StoryboardDraftEditCommand command = StoryboardDraftEditCommand.builder()
                    .title("Düzenlenmiş")
                    .build();

            service.editStoryboardDraft(draft.getId(), command, "admin");

            ArgumentCaptor<StoryboardDraft> captor = ArgumentCaptor.forClass(StoryboardDraft.class);
            verify(storyboardDraftRepository).save(captor.capture());
            assertThat(captor.getValue().getVersion()).isEqualTo(1); // version değişmedi
        }

        @Test
        @DisplayName("33. Media generation alanları yoktur")
        void shouldNotHaveMediaGenerationFields() {
            java.lang.reflect.Field[] fields = StoryboardReviewService.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("imagegeneration");
                assertThat(name).doesNotContain("videogeneration");
                assertThat(name).doesNotContain("audiogeneration");
            }
        }
    }

    // ── Status Transition Matrix ────────────────────────────

    @Nested
    @DisplayName("Disallowed status transitions")
    class DisallowedTransitions {

        @Test
        @DisplayName("34. FAILED → APPROVED disallowed")
        void failedToApprovedDisallowed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.FAILED);
            setupMock(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(draft.getId(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("35. FAILED → NEEDS_REVIEW (via edit) disallowed")
        void failedToNeedsReviewDisallowed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.FAILED);
            setupMock(draft);

            assertThatThrownBy(() -> service.editStoryboardDraft(
                    draft.getId(), StoryboardDraftEditCommand.builder().build(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("36. FAILED → REJECTED disallowed")
        void failedToRejectedDisallowed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.FAILED);
            setupMock(draft);

            assertThatThrownBy(() -> service.rejectStoryboardDraft(
                    draft.getId(), "admin", "reason"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("37. REJECTED → APPROVED disallowed")
        void rejectedToApprovedDisallowed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.REJECTED);
            setupMock(draft);

            assertThatThrownBy(() -> service.approveStoryboardDraft(draft.getId(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("38. APPROVED → NEEDS_REVIEW (via edit) disallowed")
        void approvedToNeedsReviewDisallowed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.APPROVED);
            setupMock(draft);

            assertThatThrownBy(() -> service.editStoryboardDraft(
                    draft.getId(), StoryboardDraftEditCommand.builder().build(), "admin"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("39. APPROVED → REJECTED disallowed")
        void approvedToRejectedDisallowed() {
            StoryboardDraft draft = createDraftWithStatus(StoryboardDraftStatus.APPROVED);
            setupMock(draft);

            assertThatThrownBy(() -> service.rejectStoryboardDraft(
                    draft.getId(), "admin", "reason"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Helper Methods ──────────────────────────────────────

    private StoryboardDraft createAiGeneratedDraft() {
        StoryboardDraft draft = StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .sourceScenarioDraft(approvedScenario)
                .sourceScenarioVersion(1)
                .version(1)
                .status(StoryboardDraftStatus.AI_GENERATED)
                .title("Göbeklitepe — Storyboard")
                .summary("Göbeklitepe storyboard özeti")
                .targetPlatform("YouTube")
                .aspectRatio("16:9")
                .estimatedDurationSeconds(340)
                .visualStyleGuide("Tarihi dramatik tarz")
                .narrationStyle("Hikâye anlatıcı")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType("MOCK")
                .modelName("mock-storyboard-v1")
                .build();

        // 6 scene ekle
        addValidScenesToDraft(draft);
        return draft;
    }

    private StoryboardDraft createDraftWithStatus(StoryboardDraftStatus status) {
        return StoryboardDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .sourceScenarioDraft(approvedScenario)
                .sourceScenarioVersion(1)
                .version(1)
                .status(status)
                .title("Test Storyboard")
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    private void addValidScenesToDraft(StoryboardDraft draft) {
        List<StoryboardScene> scenes = new ArrayList<>();
        int i = 1;
        for (ScenarioBeatType beatType : ScenarioBeatType.values()) {
            StoryboardScene scene = StoryboardScene.builder()
                    .id(UUID.randomUUID())
                    .storyboardDraft(draft)
                    .sceneNumber(i)
                    .sourceBeatType(beatType)
                    .sourceBeatOrder(beatType.getOrder())
                    .sceneTitle("Scene " + i + " — " + beatType.getLabel())
                    .visualDescription("Visual description for scene " + i)
                    .narrationText("Narration for scene " + i)
                    .cameraDirection("Standard shot")
                    .transitionNote("dissolve")
                    .mood("Neutral")
                    .location("Anadolu")
                    .characters("Anlatıcı")
                    .estimatedDurationSeconds(30)
                    .build();
            scenes.add(scene);
            i++;
        }
        draft.getScenes().addAll(scenes);
    }

    private void setupMock(StoryboardDraft draft) {
        when(storyboardDraftRepository.findById(draft.getId()))
                .thenReturn(Optional.of(draft));
        lenient().when(storyboardDraftRepository.save(any(StoryboardDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void setupMockForApproval(StoryboardDraft draft) {
        setupMock(draft);
        lenient().when(storyboardDraftRepository.findByContentProjectIdAndStatus(
                project.getId(), StoryboardDraftStatus.APPROVED))
                .thenReturn(List.of()); // henüz approved yok
    }
}
