package com.anadoluatlasi.backend.application.knowledge;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemType;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-001: KnowledgeItemService unit testleri.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeItemServiceTest {

    @Mock
    private KnowledgeItemRepository repository;

    @InjectMocks
    private KnowledgeItemService service;

    private KnowledgeItem draftItem;
    private KnowledgeItem readyItem;
    private KnowledgeItem archivedItem;

    @BeforeEach
    void setUp() {
        draftItem = KnowledgeItem.builder()
                .id(UUID.randomUUID())
                .title("Ankara Kalesi")
                .slug("ankara-kalesi")
                .type(KnowledgeItemType.HISTORICAL_EVENT)
                .status(KnowledgeItemStatus.DRAFT)
                .language("tr")
                .build();

        readyItem = KnowledgeItem.builder()
                .id(UUID.randomUUID())
                .title("Çanakkale Türküsü")
                .slug("canakkale-turkusu")
                .type(KnowledgeItemType.FOLK_SONG)
                .status(KnowledgeItemStatus.READY)
                .language("tr")
                .build();

        archivedItem = KnowledgeItem.builder()
                .id(UUID.randomUUID())
                .title("Eski Kayıt")
                .slug("eski-kayit")
                .type(KnowledgeItemType.TRADITION)
                .status(KnowledgeItemStatus.ARCHIVED)
                .language("tr")
                .build();
    }

    // ── Create ──────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Yeni bilgi öğesi DRAFT statüsünde oluşturulur")
        void shouldCreateWithDraftStatus() {
            CreateKnowledgeItemCommand command = CreateKnowledgeItemCommand.builder()
                    .title("Yeni Konu")
                    .type("FOLK_SONG")
                    .language("tr")
                    .summary("Özet")
                    .build();

            when(repository.findBySlug(any())).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocation -> {
                KnowledgeItem item = invocation.getArgument(0);
                item.setId(UUID.randomUUID());
                return item;
            });

            KnowledgeItemResult result = service.create(command);

            assertThat(result.getStatus()).isEqualTo("DRAFT");
            assertThat(result.getTitle()).isEqualTo("Yeni Konu");
            assertThat(result.getType()).isEqualTo("FOLK_SONG");

            ArgumentCaptor<KnowledgeItem> captor = ArgumentCaptor.forClass(KnowledgeItem.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(KnowledgeItemStatus.DRAFT);
        }

        @Test
        @DisplayName("Boş başlık ile oluşturma ValidationException fırlatır")
        void shouldRejectBlankTitle() {
            CreateKnowledgeItemCommand command = CreateKnowledgeItemCommand.builder()
                    .title("")
                    .type("FOLK_SONG")
                    .language("tr")
                    .build();

            assertThatThrownBy(() -> service.create(command))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Geçersiz tür ile oluşturma ValidationException fırlatır")
        void shouldRejectInvalidType() {
            CreateKnowledgeItemCommand command = CreateKnowledgeItemCommand.builder()
                    .title("Test")
                    .type("INVALID_TYPE")
                    .language("tr")
                    .build();

            when(repository.findBySlug(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(command))
                    .isInstanceOf(ValidationException.class);
        }
    }

    // ── Update ──────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("DRAFT öğe başarıyla güncellenir")
        void shouldUpdateDraftItem() {
            when(repository.findById(draftItem.getId())).thenReturn(Optional.of(draftItem));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            UpdateKnowledgeItemCommand command = UpdateKnowledgeItemCommand.builder()
                    .title("Ankara Kalesi")
                    .summary("Güncel özet")
                    .build();

            KnowledgeItemResult result = service.update(draftItem.getId(), command);

            assertThat(result.getSummary()).isEqualTo("Güncel özet");
        }

        @Test
        @DisplayName("ARCHIVED öğe güncellenemez")
        void shouldRejectUpdateForArchived() {
            when(repository.findById(archivedItem.getId())).thenReturn(Optional.of(archivedItem));

            UpdateKnowledgeItemCommand command = UpdateKnowledgeItemCommand.builder()
                    .title("Yeni Başlık")
                    .build();

            assertThatThrownBy(() -> service.update(archivedItem.getId(), command))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Get Detail ──────────────────────────────────────────

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("Mevcut öğe döndürülür")
        void shouldReturnExistingItem() {
            when(repository.findById(draftItem.getId())).thenReturn(Optional.of(draftItem));

            KnowledgeItemResult result = service.getDetail(draftItem.getId());

            assertThat(result.getId()).isEqualTo(draftItem.getId());
            assertThat(result.getTitle()).isEqualTo("Ankara Kalesi");
        }

        @Test
        @DisplayName("Olmayan öğe NotFoundException fırlatır")
        void shouldThrowForMissingItem() {
            UUID missingId = UUID.randomUUID();
            when(repository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDetail(missingId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── List ─────────────────────────────────────────────────

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("Tüm öğeler listelenir")
        void shouldListAll() {
            when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draftItem, readyItem));

            List<KnowledgeItemResult> results = service.list(null);

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Status filtresi uygulanır")
        void shouldFilterByStatus() {
            when(repository.findByStatus(KnowledgeItemStatus.READY)).thenReturn(List.of(readyItem));

            List<KnowledgeItemResult> results = service.list(KnowledgeItemStatus.READY);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo("READY");
        }
    }

    // ── Mark Ready ──────────────────────────────────────────

    @Nested
    @DisplayName("markReady")
    class MarkReady {

        @Test
        @DisplayName("DRAFT → READY geçişi başarılı")
        void shouldMarkDraftAsReady() {
            when(repository.findById(draftItem.getId())).thenReturn(Optional.of(draftItem));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            KnowledgeItemResult result = service.markReady(draftItem.getId());

            assertThat(result.getStatus()).isEqualTo("READY");
        }

        @Test
        @DisplayName("READY → READY geçişi BusinessException fırlatır")
        void shouldRejectReadyToReady() {
            when(repository.findById(readyItem.getId())).thenReturn(Optional.of(readyItem));

            assertThatThrownBy(() -> service.markReady(readyItem.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("ARCHIVED → READY geçişi BusinessException fırlatır")
        void shouldRejectArchivedToReady() {
            when(repository.findById(archivedItem.getId())).thenReturn(Optional.of(archivedItem));

            assertThatThrownBy(() -> service.markReady(archivedItem.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── Archive ─────────────────────────────────────────────

    @Nested
    @DisplayName("archive")
    class Archive {

        @Test
        @DisplayName("DRAFT → ARCHIVED geçişi başarılı")
        void shouldArchiveDraft() {
            when(repository.findById(draftItem.getId())).thenReturn(Optional.of(draftItem));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            KnowledgeItemResult result = service.archive(draftItem.getId());

            assertThat(result.getStatus()).isEqualTo("ARCHIVED");
        }

        @Test
        @DisplayName("READY → ARCHIVED geçişi başarılı")
        void shouldArchiveReady() {
            when(repository.findById(readyItem.getId())).thenReturn(Optional.of(readyItem));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            KnowledgeItemResult result = service.archive(readyItem.getId());

            assertThat(result.getStatus()).isEqualTo("ARCHIVED");
        }

        @Test
        @DisplayName("ARCHIVED → ARCHIVED tekrar çağrısı BusinessException fırlatır")
        void shouldRejectDoubleArchive() {
            when(repository.findById(archivedItem.getId())).thenReturn(Optional.of(archivedItem));

            assertThatThrownBy(() -> service.archive(archivedItem.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
