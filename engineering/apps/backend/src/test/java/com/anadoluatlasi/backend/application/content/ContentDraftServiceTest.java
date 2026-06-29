package com.anadoluatlasi.backend.application.content;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-001: ContentDraftService unit testleri.
 * Kritik kural: Draft update hiçbir koşulda ContentProject statüsünü değiştirmez.
 */
@ExtendWith(MockitoExtension.class)
class ContentDraftServiceTest {

    @Mock
    private ContentDraftRepository contentDraftRepository;

    @InjectMocks
    private ContentDraftService service;

    private ContentProject project;
    private ContentDraft generatedDraft;
    private ContentDraft revisionNeededDraft;
    private ContentDraft editedDraft;
    private ContentDraft approvedDraft;

    @BeforeEach
    void setUp() {
        project = ContentProject.builder()
                .id(UUID.randomUUID())
                .status(ContentProjectStatus.DRAFT_READY)
                .language("tr")
                .retryCount(0)
                .build();

        generatedDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("AI Başlık")
                .body("AI tarafından üretilen içerik metni.")
                .summary("Özet")
                .status(ContentDraftStatus.GENERATED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        revisionNeededDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Revize Başlık")
                .body("Revize edilmesi gereken içerik.")
                .status(ContentDraftStatus.REVISION_NEEDED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        editedDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Düzenlenmiş Başlık")
                .body("Düzenlenmiş içerik.")
                .status(ContentDraftStatus.EDITED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();

        approvedDraft = ContentDraft.builder()
                .id(UUID.randomUUID())
                .contentProject(project)
                .title("Onaylanmış Başlık")
                .body("Onaylanmış içerik.")
                .status(ContentDraftStatus.APPROVED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
    }

    // ── Get Detail ──────────────────────────────────────────

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("Mevcut draft döndürülür")
        void shouldReturnExistingDraft() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));

            ContentDraftResult result = service.getDetail(generatedDraft.getId());

            assertThat(result.getId()).isEqualTo(generatedDraft.getId());
            assertThat(result.getTitle()).isEqualTo("AI Başlık");
            assertThat(result.getStatus()).isEqualTo("GENERATED");
        }

        @Test
        @DisplayName("Olmayan draft NotFoundException fırlatır")
        void shouldThrowForMissing() {
            UUID missingId = UUID.randomUUID();
            when(contentDraftRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDetail(missingId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── Update — Başarılı ───────────────────────────────────

    @Nested
    @DisplayName("update — başarılı")
    class UpdateSuccess {

        @Test
        @DisplayName("GENERATED draft başarıyla güncellenir")
        void shouldUpdateGeneratedDraft() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));
            when(contentDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .title("Yeni Başlık")
                    .body("Yeni içerik metni.")
                    .build();

            ContentDraftResult result = service.update(generatedDraft.getId(), command);

            assertThat(result.getTitle()).isEqualTo("Yeni Başlık");
            assertThat(result.getBody()).isEqualTo("Yeni içerik metni.");
            assertThat(result.getStatus()).isEqualTo("EDITED");
        }

        @Test
        @DisplayName("REVISION_NEEDED draft başarıyla güncellenir")
        void shouldUpdateRevisionNeededDraft() {
            when(contentDraftRepository.findById(revisionNeededDraft.getId()))
                    .thenReturn(Optional.of(revisionNeededDraft));
            when(contentDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .body("Revize edilmiş içerik.")
                    .build();

            ContentDraftResult result = service.update(revisionNeededDraft.getId(), command);

            assertThat(result.getStatus()).isEqualTo("EDITED");
        }

        @Test
        @DisplayName("Partial update — sadece body güncellenir, title değişmez")
        void shouldUpdatePartially() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));
            when(contentDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .body("Sadece body güncellendi.")
                    .build();

            ContentDraftResult result = service.update(generatedDraft.getId(), command);

            assertThat(result.getTitle()).isEqualTo("AI Başlık"); // title değişmedi
            assertThat(result.getBody()).isEqualTo("Sadece body güncellendi.");
        }

        @Test
        @DisplayName("Update sonrası draft EDITED statüsüne geçer")
        void shouldSetEditedStatus() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));
            when(contentDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .summary("Yeni özet")
                    .build();

            service.update(generatedDraft.getId(), command);

            ArgumentCaptor<ContentDraft> captor = ArgumentCaptor.forClass(ContentDraft.class);
            verify(contentDraftRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ContentDraftStatus.EDITED);
        }
    }

    // ── Update — Reddedilen ─────────────────────────────────

    @Nested
    @DisplayName("update — reddedilen durumlar")
    class UpdateRejected {

        @Test
        @DisplayName("EDITED draft düzenlenemez")
        void shouldRejectEditedDraft() {
            when(contentDraftRepository.findById(editedDraft.getId()))
                    .thenReturn(Optional.of(editedDraft));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .title("Yeni")
                    .build();

            assertThatThrownBy(() -> service.update(editedDraft.getId(), command))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("APPROVED draft düzenlenemez")
        void shouldRejectApprovedDraft() {
            when(contentDraftRepository.findById(approvedDraft.getId()))
                    .thenReturn(Optional.of(approvedDraft));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .title("Yeni")
                    .build();

            assertThatThrownBy(() -> service.update(approvedDraft.getId(), command))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Boş title gönderilirse ValidationException")
        void shouldRejectBlankTitle() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .title("   ")
                    .build();

            assertThatThrownBy(() -> service.update(generatedDraft.getId(), command))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Boş body gönderilirse ValidationException")
        void shouldRejectBlankBody() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .body("")
                    .build();

            assertThatThrownBy(() -> service.update(generatedDraft.getId(), command))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Hiçbir alan verilmezse ValidationException")
        void shouldRejectEmptyUpdate() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder().build();

            assertThatThrownBy(() -> service.update(generatedDraft.getId(), command))
                    .isInstanceOf(ValidationException.class);
        }
    }

    // ── Update — ContentProject statüsü değişmez ───────────

    @Nested
    @DisplayName("update — ContentProject isolation")
    class UpdateProjectIsolation {

        @Test
        @DisplayName("Draft update ContentProject statüsünü değiştirmez")
        void shouldNotChangeProjectStatus() {
            ContentProjectStatus originalStatus = project.getStatus();

            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));
            when(contentDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                    .title("Yeni Başlık")
                    .build();

            service.update(generatedDraft.getId(), command);

            // ContentProject statüsü değişmedi
            assertThat(project.getStatus()).isEqualTo(originalStatus);
        }

        @Test
        @DisplayName("Draft update asla APPROVED yapmaz")
        void shouldNeverApproveProject() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));
            when(contentDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.update(generatedDraft.getId(), UpdateContentDraftCommand.builder().title("X").build());

            assertThat(project.getStatus()).isNotEqualTo(ContentProjectStatus.APPROVED);
        }

        @Test
        @DisplayName("Draft update asla READY_FOR_PUBLISH yapmaz")
        void shouldNeverPublishProject() {
            when(contentDraftRepository.findById(generatedDraft.getId()))
                    .thenReturn(Optional.of(generatedDraft));
            when(contentDraftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.update(generatedDraft.getId(), UpdateContentDraftCommand.builder().body("Y").build());

            assertThat(project.getStatus()).isNotEqualTo(ContentProjectStatus.READY_FOR_PUBLISH);
        }
    }
}
