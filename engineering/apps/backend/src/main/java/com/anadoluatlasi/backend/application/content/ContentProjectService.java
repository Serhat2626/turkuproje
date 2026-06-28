package com.anadoluatlasi.backend.application.content;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.ContentPurpose;
import com.anadoluatlasi.backend.content.entity.ContentTone;
import com.anadoluatlasi.backend.content.entity.TargetAudience;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * FP-001: Content Project yaşam döngüsünü yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>create — yeni proje oluşturma (CREATED statüsünde)</li>
 *   <li>getDetail — tekil proje detayı</li>
 *   <li>list — proje listeleme (opsiyonel knowledgeItemId ve status filtre)</li>
 *   <li>startReview — DRAFT_READY → IN_REVIEW</li>
 *   <li>approve — IN_REVIEW → APPROVED</li>
 *   <li>requestRevision — IN_REVIEW/DRAFT_READY → REVISION_REQUESTED</li>
 *   <li>reject — IN_REVIEW → REJECTED</li>
 *   <li>markReadyForPublish — APPROVED → READY_FOR_PUBLISH</li>
 *   <li>cancel — ara statülerden → CANCELLED</li>
 * </ul>
 *
 * <p>Bu service AI üretimi, retry veya gateway çağrısı yapmaz.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProjectService {

    private final ContentProjectRepository contentProjectRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;

    /**
     * Cancel yapılabilecek ara statüler.
     * Final statüler (APPROVED, REJECTED, READY_FOR_PUBLISH, CANCELLED) ve FAILED hariç.
     */
    private static final Set<ContentProjectStatus> CANCELLABLE_STATUSES = Set.of(
            ContentProjectStatus.CREATED,
            ContentProjectStatus.AI_REQUESTED,
            ContentProjectStatus.AI_PROCESSING,
            ContentProjectStatus.DRAFT_READY,
            ContentProjectStatus.IN_REVIEW,
            ContentProjectStatus.REVISION_REQUESTED
    );

    // ── Create ──────────────────────────────────────────────

    /**
     * Yeni bir içerik projesi oluşturur.
     * Sadece READY statüdeki Knowledge Item'dan proje oluşturulabilir.
     */
    @Transactional
    public ContentProjectResult create(CreateContentProjectCommand command) {
        // 1. KnowledgeItem doğrulaması
        if (command.getKnowledgeItemId() == null) {
            throw new ValidationException("Bilgi öğesi ID boş olamaz");
        }

        KnowledgeItem knowledgeItem = knowledgeItemRepository.findById(command.getKnowledgeItemId())
                .orElseThrow(() -> new NotFoundException("KnowledgeItem", command.getKnowledgeItemId()));

        if (knowledgeItem.getStatus() == KnowledgeItemStatus.ARCHIVED) {
            throw new BusinessException("Arşivlenmiş bilgi öğesinden içerik projesi oluşturulamaz: "
                    + command.getKnowledgeItemId());
        }
        if (knowledgeItem.getStatus() != KnowledgeItemStatus.READY) {
            throw new BusinessException(
                    String.format("İçerik projesi sadece READY bilgi öğesinden oluşturulabilir. Mevcut statü: %s",
                            knowledgeItem.getStatus()));
        }

        // 2. Giriş validasyonu
        if (command.getLanguage() == null || command.getLanguage().isBlank()) {
            throw new ValidationException("Dil boş olamaz");
        }

        // 3. Entity oluştur
        ContentProject entity = ContentProject.builder()
                .knowledgeItem(knowledgeItem)
                .contentPurpose(resolveEnum(ContentPurpose.class, command.getContentPurpose(), "contentPurpose"))
                .targetAudience(resolveEnum(TargetAudience.class, command.getTargetAudience(), "targetAudience"))
                .tone(resolveEnum(ContentTone.class, command.getTone(), "tone"))
                .language(command.getLanguage())
                .formatHint(command.getFormatHint())
                .lengthHint(command.getLengthHint())
                .status(ContentProjectStatus.CREATED)
                .build();

        ContentProject saved = contentProjectRepository.save(entity);
        log.info("İçerik projesi oluşturuldu: id={}, knowledgeItemId={}",
                saved.getId(), command.getKnowledgeItemId());

        return toResult(saved, knowledgeItem);
    }

    // ── Get Detail ──────────────────────────────────────────

    /**
     * Tekil içerik projesi detayını döndürür.
     */
    @Transactional(readOnly = true)
    public ContentProjectResult getDetail(UUID id) {
        ContentProject entity = findOrThrow(id);
        return toResult(entity, entity.getKnowledgeItem());
    }

    // ── List ─────────────────────────────────────────────────

    /**
     * İçerik projelerini listeler.
     * Opsiyonel filtreler: knowledgeItemId, status.
     */
    @Transactional(readOnly = true)
    public List<ContentProjectResult> list(UUID knowledgeItemId, ContentProjectStatus statusFilter) {
        List<ContentProject> projects;

        if (knowledgeItemId != null && statusFilter != null) {
            projects = contentProjectRepository.findByKnowledgeItemIdAndStatus(knowledgeItemId, statusFilter);
        } else if (knowledgeItemId != null) {
            projects = contentProjectRepository.findByKnowledgeItemId(knowledgeItemId);
        } else if (statusFilter != null) {
            projects = contentProjectRepository.findByStatus(statusFilter);
        } else {
            projects = contentProjectRepository.findAllByOrderByCreatedAtDesc();
        }

        return projects.stream()
                .map(p -> toResult(p, p.getKnowledgeItem()))
                .toList();
    }

    // ── Start Review ────────────────────────────────────────

    /**
     * DRAFT_READY → IN_REVIEW geçişi.
     * AI draft'ı insan review'a alır.
     */
    @Transactional
    public ContentProjectResult startReview(UUID id) {
        ContentProject entity = findOrThrow(id);
        assertStatus(entity, ContentProjectStatus.DRAFT_READY, "startReview");

        entity.setStatus(ContentProjectStatus.IN_REVIEW);
        ContentProject saved = contentProjectRepository.save(entity);
        log.info("İçerik projesi review'a alındı: id={}", saved.getId());

        return toResult(saved, saved.getKnowledgeItem());
    }

    // ── Approve ─────────────────────────────────────────────

    /**
     * IN_REVIEW → APPROVED geçişi.
     * İnsan editör draft'ı onaylar.
     *
     * @param id proje ID
     * @param reviewNote opsiyonel onay notu
     */
    @Transactional
    public ContentProjectResult approve(UUID id, String reviewNote) {
        ContentProject entity = findOrThrow(id);
        assertStatus(entity, ContentProjectStatus.IN_REVIEW, "approve");

        entity.setStatus(ContentProjectStatus.APPROVED);
        ContentProject saved = contentProjectRepository.save(entity);
        log.info("İçerik projesi onaylandı: id={}, note={}", saved.getId(),
                reviewNote != null ? reviewNote : "(yok)");

        return toResult(saved, saved.getKnowledgeItem());
    }

    // ── Request Revision ────────────────────────────────────

    /**
     * IN_REVIEW/DRAFT_READY → REVISION_REQUESTED geçişi.
     * İnsan editör yeni AI üretimi talep eder.
     *
     * @param id proje ID
     * @param reviewNote zorunlu revizyon notu
     */
    @Transactional
    public ContentProjectResult requestRevision(UUID id, String reviewNote) {
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new ValidationException("Revizyon notu boş olamaz");
        }

        ContentProject entity = findOrThrow(id);

        if (entity.getStatus() != ContentProjectStatus.IN_REVIEW
                && entity.getStatus() != ContentProjectStatus.DRAFT_READY) {
            throw new BusinessException(
                    String.format("Revizyon talebi yapılamaz. Mevcut statü: %s, Beklenen: IN_REVIEW veya DRAFT_READY",
                            entity.getStatus()));
        }

        entity.setStatus(ContentProjectStatus.REVISION_REQUESTED);
        ContentProject saved = contentProjectRepository.save(entity);
        log.info("Revizyon talep edildi: id={}, note={}", saved.getId(), reviewNote);

        return toResult(saved, saved.getKnowledgeItem());
    }

    // ── Reject ──────────────────────────────────────────────

    /**
     * IN_REVIEW → REJECTED geçişi.
     * İnsan editör draft'ı reddeder.
     *
     * @param id proje ID
     * @param reviewNote zorunlu red notu
     */
    @Transactional
    public ContentProjectResult reject(UUID id, String reviewNote) {
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new ValidationException("Red notu boş olamaz");
        }

        ContentProject entity = findOrThrow(id);
        assertStatus(entity, ContentProjectStatus.IN_REVIEW, "reject");

        entity.setStatus(ContentProjectStatus.REJECTED);
        ContentProject saved = contentProjectRepository.save(entity);
        log.info("İçerik projesi reddedildi: id={}, note={}", saved.getId(), reviewNote);

        return toResult(saved, saved.getKnowledgeItem());
    }

    // ── Mark Ready For Publish ──────────────────────────────

    /**
     * APPROVED → READY_FOR_PUBLISH geçişi.
     * İnsan review'dan geçmiş onaylı projeyi yayına hazır işaretler.
     */
    @Transactional
    public ContentProjectResult markReadyForPublish(UUID id) {
        ContentProject entity = findOrThrow(id);
        assertStatus(entity, ContentProjectStatus.APPROVED, "markReadyForPublish");

        entity.setStatus(ContentProjectStatus.READY_FOR_PUBLISH);
        ContentProject saved = contentProjectRepository.save(entity);
        log.info("İçerik projesi yayına hazır: id={}", saved.getId());

        return toResult(saved, saved.getKnowledgeItem());
    }

    // ── Cancel ──────────────────────────────────────────────

    /**
     * Ara statülerden → CANCELLED geçişi.
     * Final statüler (APPROVED, REJECTED, READY_FOR_PUBLISH, CANCELLED) ve FAILED'dan iptal yapılamaz.
     */
    @Transactional
    public ContentProjectResult cancel(UUID id) {
        ContentProject entity = findOrThrow(id);

        if (!CANCELLABLE_STATUSES.contains(entity.getStatus())) {
            throw new BusinessException(
                    String.format("Proje iptal edilemez. Mevcut statü: %s", entity.getStatus()));
        }

        entity.setStatus(ContentProjectStatus.CANCELLED);
        ContentProject saved = contentProjectRepository.save(entity);
        log.info("İçerik projesi iptal edildi: id={}", saved.getId());

        return toResult(saved, saved.getKnowledgeItem());
    }

    // ── Private Helpers ─────────────────────────────────────

    private ContentProject findOrThrow(UUID id) {
        return contentProjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentProject", id));
    }

    private void assertStatus(ContentProject entity, ContentProjectStatus expected, String operation) {
        if (entity.getStatus() != expected) {
            throw new BusinessException(
                    String.format("%s yapılamaz. Mevcut statü: %s, Beklenen: %s",
                            operation, entity.getStatus(), expected));
        }
    }

    private <E extends Enum<E>> E resolveEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Geçersiz " + fieldName + " değeri: " + value);
        }
    }

    private ContentProjectResult toResult(ContentProject entity, KnowledgeItem ki) {
        return ContentProjectResult.builder()
                .id(entity.getId())
                .knowledgeItemId(ki != null ? ki.getId() : null)
                .knowledgeItemTitle(ki != null ? ki.getTitle() : null)
                .contentPurpose(entity.getContentPurpose() != null ? entity.getContentPurpose().name() : null)
                .targetAudience(entity.getTargetAudience() != null ? entity.getTargetAudience().name() : null)
                .tone(entity.getTone() != null ? entity.getTone().name() : null)
                .language(entity.getLanguage())
                .formatHint(entity.getFormatHint())
                .lengthHint(entity.getLengthHint())
                .status(entity.getStatus().name())
                .retryCount(entity.getRetryCount())
                .lastErrorType(entity.getLastErrorType() != null ? entity.getLastErrorType().name() : null)
                .lastErrorMessage(entity.getLastErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
