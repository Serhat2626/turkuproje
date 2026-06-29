package com.anadoluatlasi.backend.application.content;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * FP-001: Content Draft yaşam döngüsünü yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>getDetail — tekil draft detayı</li>
 *   <li>update — manuel editör düzenlemesi (title, body, summary)</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Draft'ı otomatik onaylamaz (APPROVED yapmaz)</li>
 *   <li>Content Project statüsünü değiştirmez</li>
 *   <li>Projeyi READY_FOR_PUBLISH yapmaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentDraftService {

    private final ContentDraftRepository contentDraftRepository;

    /**
     * Manuel düzenleme yapılabilecek draft statüleri.
     */
    private static final Set<ContentDraftStatus> EDITABLE_STATUSES = Set.of(
            ContentDraftStatus.GENERATED,
            ContentDraftStatus.REVISION_NEEDED
    );

    // ── Get Detail ──────────────────────────────────────────

    /**
     * Tekil draft detayını döndürür.
     */
    @Transactional(readOnly = true)
    public ContentDraftResult getDetail(UUID draftId) {
        ContentDraft entity = findOrThrow(draftId);
        return toResult(entity);
    }

    // ── Update ──────────────────────────────────────────────

    /**
     * Draft'ı manuel olarak düzenler.
     * Sadece GENERATED veya REVISION_NEEDED statüsündeki draft'lar düzenlenebilir.
     * Düzenleme sonrası draft statüsü EDITED olur.
     * ContentProject statüsü değiştirilmez.
     */
    @Transactional
    public ContentDraftResult update(UUID draftId, UpdateContentDraftCommand command) {
        ContentDraft entity = findOrThrow(draftId);

        // Statü kontrolü
        if (!EDITABLE_STATUSES.contains(entity.getStatus())) {
            throw new BusinessException(
                    String.format("Draft düzenlenemez. Mevcut statü: %s, İzin verilen: %s",
                            entity.getStatus(), EDITABLE_STATUSES));
        }

        // Validasyon
        boolean hasChange = false;

        if (command.getTitle() != null) {
            if (command.getTitle().isBlank()) {
                throw new ValidationException("Başlık boş olamaz");
            }
            entity.setTitle(command.getTitle());
            hasChange = true;
        }

        if (command.getBody() != null) {
            if (command.getBody().isBlank()) {
                throw new ValidationException("İçerik metni boş olamaz");
            }
            entity.setBody(command.getBody());
            hasChange = true;
        }

        if (command.getSummary() != null) {
            entity.setSummary(command.getSummary());
            hasChange = true;
        }

        if (!hasChange) {
            throw new ValidationException("En az bir alan güncellenmelidir");
        }

        // Statüyü EDITED yap
        entity.setStatus(ContentDraftStatus.EDITED);

        ContentDraft saved = contentDraftRepository.save(entity);
        log.info("Draft düzenlendi: draftId={}, yeni statü=EDITED", saved.getId());

        return toResult(saved);
    }

    // ── Private Helpers ─────────────────────────────────────

    private ContentDraft findOrThrow(UUID id) {
        return contentDraftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentDraft", id));
    }

    private ContentDraftResult toResult(ContentDraft entity) {
        return ContentDraftResult.builder()
                .id(entity.getId())
                .contentProjectId(entity.getContentProject() != null
                        ? entity.getContentProject().getId() : null)
                .title(entity.getTitle())
                .body(entity.getBody())
                .summary(entity.getSummary())
                .status(entity.getStatus().name())
                .generatedBy(entity.getGeneratedBy().name())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
