package com.anadoluatlasi.backend.application.content;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.ContentPurpose;
import com.anadoluatlasi.backend.content.entity.ContentTone;
import com.anadoluatlasi.backend.content.entity.TargetAudience;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemType;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * FP-001: ContentProjectService unit testleri.
 */
@ExtendWith(MockitoExtension.class)
class ContentProjectServiceTest {

    @Mock
    private ContentProjectRepository contentProjectRepository;

    @Mock
    private ContentDraftRepository contentDraftRepository;

    @Mock
    private KnowledgeItemRepository knowledgeItemRepository;

    @InjectMocks
    private ContentProjectService service;

    private KnowledgeItem readyKI;
    private KnowledgeItem archivedKI;
    private KnowledgeItem draftKI;

    @BeforeEach
    void setUp() {
        readyKI = KnowledgeItem.builder()
                .id(UUID.randomUUID())
                .title("Ankara Kalesi")
                .slug("ankara-kalesi")
                .type(KnowledgeItemType.HISTORICAL_EVENT)
                .status(KnowledgeItemStatus.READY)
                .language("tr")
                .build();

        archivedKI = KnowledgeItem.builder()
                .id(UUID.randomUUID())
                .title("Eski Kayıt")
                .slug("eski-kayit")
                .type(KnowledgeItemType.TRADITION)
                .status(KnowledgeItemStatus.ARCHIVED)
                .language("tr")
                .build();

        draftKI = KnowledgeItem.builder()
                .id(UUID.randomUUID())
                .title("Taslak Kayıt")
                .slug("taslak-kayit")
                .type(KnowledgeItemType.FOLK_SONG)
                .status(KnowledgeItemStatus.DRAFT)
                .language("tr")
                .build();
    }

    private ContentProject buildProject(ContentProjectStatus status) {
        return ContentProject.builder()
                .id(UUID.randomUUID())
                .knowledgeItem(readyKI)
                .contentPurpose(ContentPurpose.INFORMATIONAL_TEXT)
                .targetAudience(TargetAudience.GENERAL_USER)
                .tone(ContentTone.INFORMATIVE)
                .language("tr")
                .status(status)
                .retryCount(0)
                .build();
    }

    // ── Create ──────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("READY KI'dan proje CREATED statüsünde oluşturulur")
        void shouldCreateFromReadyKI() {
            when(knowledgeItemRepository.findById(readyKI.getId())).thenReturn(Optional.of(readyKI));
            when(contentProjectRepository.save(any())).thenAnswer(inv -> {
                ContentProject p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            CreateContentProjectCommand command = CreateContentProjectCommand.builder()
                    .knowledgeItemId(readyKI.getId())
                    .contentPurpose("INFORMATIONAL_TEXT")
                    .targetAudience("GENERAL_USER")
                    .tone("INFORMATIVE")
                    .language("tr")
                    .build();

            ContentProjectResult result = service.create(command);

            assertThat(result.getStatus()).isEqualTo("CREATED");
            assertThat(result.getKnowledgeItemId()).isEqualTo(readyKI.getId());
        }

        @Test
        @DisplayName("ARCHIVED KI'dan proje oluşturulamaz")
        void shouldRejectArchivedKI() {
            when(knowledgeItemRepository.findById(archivedKI.getId())).thenReturn(Optional.of(archivedKI));

            CreateContentProjectCommand command = CreateContentProjectCommand.builder()
                    .knowledgeItemId(archivedKI.getId())
                    .language("tr")
                    .build();

            assertThatThrownBy(() -> service.create(command))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("DRAFT KI'dan proje oluşturulamaz")
        void shouldRejectDraftKI() {
            when(knowledgeItemRepository.findById(draftKI.getId())).thenReturn(Optional.of(draftKI));

            CreateContentProjectCommand command = CreateContentProjectCommand.builder()
                    .knowledgeItemId(draftKI.getId())
                    .language("tr")
                    .build();

            assertThatThrownBy(() -> service.create(command))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Olmayan KI ile proje oluşturma NotFoundException fırlatır")
        void shouldRejectMissingKI() {
            UUID missingId = UUID.randomUUID();
            when(knowledgeItemRepository.findById(missingId)).thenReturn(Optional.empty());

            CreateContentProjectCommand command = CreateContentProjectCommand.builder()
                    .knowledgeItemId(missingId)
                    .language("tr")
                    .build();

            assertThatThrownBy(() -> service.create(command))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── Start Review ────────────────────────────────────────

    @Nested
    @DisplayName("startReview")
    class StartReview {

        @Test
        @DisplayName("DRAFT_READY → IN_REVIEW geçişi başarılı")
        void shouldStartReview() {
            ContentProject project = buildProject(ContentProjectStatus.DRAFT_READY);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContentProjectResult result = service.startReview(project.getId());

            assertThat(result.getStatus()).isEqualTo("IN_REVIEW");
        }

        @Test
        @DisplayName("CREATED'den startReview yapılamaz")
        void shouldRejectFromCreated() {
            ContentProject project = buildProject(ContentProjectStatus.CREATED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.startReview(project.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Approve ─────────────────────────────────────────────

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("IN_REVIEW → APPROVED geçişi başarılı")
        void shouldApprove() {
            ContentProject project = buildProject(ContentProjectStatus.IN_REVIEW);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContentProjectResult result = service.approve(project.getId(), "İyi görünüyor");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("DRAFT_READY'den approve yapılamaz")
        void shouldRejectFromDraftReady() {
            ContentProject project = buildProject(ContentProjectStatus.DRAFT_READY);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.approve(project.getId(), null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("CREATED'den approve yapılamaz — human review bypass engeli")
        void shouldRejectFromCreated() {
            ContentProject project = buildProject(ContentProjectStatus.CREATED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.approve(project.getId(), null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Request Revision ────────────────────────────────────

    @Nested
    @DisplayName("requestRevision")
    class RequestRevision {

        @Test
        @DisplayName("IN_REVIEW → REVISION_REQUESTED geçişi başarılı")
        void shouldRequestRevisionFromInReview() {
            ContentProject project = buildProject(ContentProjectStatus.IN_REVIEW);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContentProjectResult result = service.requestRevision(project.getId(), "Daha kısa yaz");

            assertThat(result.getStatus()).isEqualTo("REVISION_REQUESTED");
        }

        @Test
        @DisplayName("Boş reviewNote ile revizyon talebi ValidationException fırlatır")
        void shouldRejectBlankNote() {
            assertThatThrownBy(() -> service.requestRevision(UUID.randomUUID(), ""))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("CREATED'den revizyon talebi yapılamaz")
        void shouldRejectFromCreated() {
            ContentProject project = buildProject(ContentProjectStatus.CREATED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.requestRevision(project.getId(), "Not"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Reject ──────────────────────────────────────────────

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("IN_REVIEW → REJECTED geçişi başarılı")
        void shouldReject() {
            ContentProject project = buildProject(ContentProjectStatus.IN_REVIEW);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContentProjectResult result = service.reject(project.getId(), "Kalitesiz içerik");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Boş reviewNote ile red ValidationException fırlatır")
        void shouldRejectBlankNote() {
            assertThatThrownBy(() -> service.reject(UUID.randomUUID(), ""))
                    .isInstanceOf(ValidationException.class);
        }
    }

    // ── Mark Ready For Publish ──────────────────────────────

    @Nested
    @DisplayName("markReadyForPublish")
    class MarkReadyForPublish {

        @Test
        @DisplayName("APPROVED → READY_FOR_PUBLISH geçişi başarılı")
        void shouldMarkReadyForPublish() {
            ContentProject project = buildProject(ContentProjectStatus.APPROVED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContentProjectResult result = service.markReadyForPublish(project.getId());

            assertThat(result.getStatus()).isEqualTo("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("DRAFT_READY'den READY_FOR_PUBLISH yapılamaz — human review zorunlu")
        void shouldRejectFromDraftReady() {
            ContentProject project = buildProject(ContentProjectStatus.DRAFT_READY);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.markReadyForPublish(project.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("CREATED'den READY_FOR_PUBLISH yapılamaz")
        void shouldRejectFromCreated() {
            ContentProject project = buildProject(ContentProjectStatus.CREATED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.markReadyForPublish(project.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("IN_REVIEW'den READY_FOR_PUBLISH yapılamaz — approve olmadan")
        void shouldRejectFromInReview() {
            ContentProject project = buildProject(ContentProjectStatus.IN_REVIEW);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.markReadyForPublish(project.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Cancel ──────────────────────────────────────────────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("CREATED → CANCELLED geçişi başarılı")
        void shouldCancelFromCreated() {
            ContentProject project = buildProject(ContentProjectStatus.CREATED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContentProjectResult result = service.cancel(project.getId());

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("APPROVED'den cancel yapılamaz")
        void shouldRejectFromApproved() {
            ContentProject project = buildProject(ContentProjectStatus.APPROVED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.cancel(project.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("READY_FOR_PUBLISH'den cancel yapılamaz")
        void shouldRejectFromReadyForPublish() {
            ContentProject project = buildProject(ContentProjectStatus.READY_FOR_PUBLISH);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.cancel(project.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("CANCELLED'den tekrar cancel yapılamaz")
        void shouldRejectFromCancelled() {
            ContentProject project = buildProject(ContentProjectStatus.CANCELLED);
            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> service.cancel(project.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Latest Draft Id (KL-001) ────────────────────────────

    @Nested
    @DisplayName("latestDraftId (KL-001)")
    class LatestDraftId {

        @Test
        @DisplayName("getDetail — draft varsa latestDraftId dolu döner")
        void shouldIncludeLatestDraftIdWhenDraftExists() {
            ContentProject project = buildProject(ContentProjectStatus.DRAFT_READY);
            UUID draftId = UUID.randomUUID();

            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));

            ContentDraft draft = ContentDraft.builder()
                    .id(draftId)
                    .contentProject(project)
                    .status(ContentDraftStatus.GENERATED)
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();
            when(contentDraftRepository.findFirstByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(Optional.of(draft));

            ContentProjectResult result = service.getDetail(project.getId());

            assertThat(result.getLatestDraftId()).isEqualTo(draftId);
            assertThat(result.getStatus()).isEqualTo("DRAFT_READY");
        }

        @Test
        @DisplayName("getDetail — draft yoksa latestDraftId null döner")
        void shouldReturnNullLatestDraftIdWhenNoDraft() {
            ContentProject project = buildProject(ContentProjectStatus.CREATED);

            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentDraftRepository.findFirstByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(Optional.empty());

            ContentProjectResult result = service.getDetail(project.getId());

            assertThat(result.getLatestDraftId()).isNull();
            assertThat(result.getStatus()).isEqualTo("CREATED");
        }

        @Test
        @DisplayName("latestDraftId proje statüsünü APPROVED yapmaz")
        void shouldNotChangeStatusToApproved() {
            ContentProject project = buildProject(ContentProjectStatus.DRAFT_READY);

            when(contentProjectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(contentDraftRepository.findFirstByContentProjectIdOrderByCreatedAtDesc(project.getId()))
                    .thenReturn(Optional.empty());

            ContentProjectResult result = service.getDetail(project.getId());

            assertThat(result.getStatus()).isNotEqualTo("APPROVED");
            assertThat(result.getStatus()).isNotEqualTo("READY_FOR_PUBLISH");
        }
    }
}
