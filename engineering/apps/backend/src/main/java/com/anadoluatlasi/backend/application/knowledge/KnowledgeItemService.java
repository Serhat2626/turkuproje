package com.anadoluatlasi.backend.application.knowledge;

import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemType;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FP-001: Knowledge Item yaşam döngüsünü yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>create — yeni bilgi öğesi oluşturma (DRAFT statüsünde)</li>
 *   <li>update — mevcut bilgi öğesini güncelleme (ARCHIVED hariç)</li>
 *   <li>getDetail — tekil bilgi öğesi detayı</li>
 *   <li>list — tüm bilgi öğelerini listeleme (opsiyonel status filtre)</li>
 *   <li>markReady — DRAFT → READY geçişi</li>
 *   <li>archive — DRAFT/READY → ARCHIVED geçişi</li>
 * </ul>
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>title zorunlu</li>
 *   <li>Yeni kayıt DRAFT statüsünde başlar</li>
 *   <li>DRAFT → READY, DRAFT → ARCHIVED, READY → ARCHIVED geçişleri geçerli</li>
 *   <li>ARCHIVED → READY yasak</li>
 *   <li>ARCHIVED öğeler güncellenemez</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeItemService {

    private final KnowledgeItemRepository knowledgeItemRepository;

    // ── Create ──────────────────────────────────────────────

    /**
     * Yeni bir bilgi öğesi oluşturur. Başlangıç statüsü DRAFT.
     */
    @Transactional
    public KnowledgeItemResult create(CreateKnowledgeItemCommand command) {
        log.debug("Bilgi öğesi oluşturuluyor: {}", command.getTitle());

        validateTitle(command.getTitle());
        validateSlugUniqueness(command.getTitle(), null);

        KnowledgeItem entity = KnowledgeItem.builder()
                .title(command.getTitle())
                .slug(generateSlug(command.getTitle()))
                .type(resolveType(command.getType()))
                .summary(command.getSummary())
                .description(command.getDescription())
                .category(command.getCategory())
                .city(command.getCity())
                .district(command.getDistrict())
                .locationText(command.getLocationText())
                .notes(command.getNotes())
                .status(KnowledgeItemStatus.DRAFT)
                .language(command.getLanguage())
                .build();

        KnowledgeItem saved = knowledgeItemRepository.save(entity);
        log.info("Bilgi öğesi oluşturuldu: id={}, title={}", saved.getId(), saved.getTitle());

        return toResult(saved);
    }

    // ── Update ──────────────────────────────────────────────

    /**
     * Mevcut bir bilgi öğesini günceller.
     * ARCHIVED statüdeki öğeler güncellenemez.
     */
    @Transactional
    public KnowledgeItemResult update(UUID id, UpdateKnowledgeItemCommand command) {
        KnowledgeItem entity = findOrThrow(id);

        if (entity.getStatus() == KnowledgeItemStatus.ARCHIVED) {
            throw new BusinessException("Arşivlenmiş bilgi öğesi güncellenemez: " + id);
        }

        validateTitle(command.getTitle());

        // Başlık değiştiyse slug'ı da güncelle ve çakışma kontrolü yap
        if (!entity.getTitle().equals(command.getTitle())) {
            validateSlugUniqueness(command.getTitle(), id);
            entity.setSlug(generateSlug(command.getTitle()));
        }

        entity.setTitle(command.getTitle());
        entity.setSummary(command.getSummary());
        entity.setDescription(command.getDescription());
        entity.setCategory(command.getCategory());
        entity.setCity(command.getCity());
        entity.setDistrict(command.getDistrict());
        entity.setLocationText(command.getLocationText());
        entity.setNotes(command.getNotes());

        if (command.getType() != null) {
            entity.setType(resolveType(command.getType()));
        }
        if (command.getLanguage() != null) {
            entity.setLanguage(command.getLanguage());
        }

        KnowledgeItem saved = knowledgeItemRepository.save(entity);
        log.info("Bilgi öğesi güncellendi: id={}", saved.getId());

        return toResult(saved);
    }

    // ── Get Detail ──────────────────────────────────────────

    /**
     * Tekil bilgi öğesi detayını döndürür.
     */
    @Transactional(readOnly = true)
    public KnowledgeItemResult getDetail(UUID id) {
        return toResult(findOrThrow(id));
    }

    // ── List ─────────────────────────────────────────────────

    /**
     * Tüm bilgi öğelerini listeler.
     * Opsiyonel olarak status filtresi uygulanabilir.
     */
    @Transactional(readOnly = true)
    public List<KnowledgeItemResult> list(KnowledgeItemStatus statusFilter) {
        List<KnowledgeItem> items;

        if (statusFilter != null) {
            items = knowledgeItemRepository.findByStatus(statusFilter);
        } else {
            items = knowledgeItemRepository.findAllByOrderByCreatedAtDesc();
        }

        return items.stream().map(this::toResult).toList();
    }

    // ── Mark Ready ──────────────────────────────────────────

    /**
     * Bilgi öğesini READY statüsüne geçirir.
     * Sadece DRAFT → READY geçişi geçerlidir.
     */
    @Transactional
    public KnowledgeItemResult markReady(UUID id) {
        KnowledgeItem entity = findOrThrow(id);

        if (entity.getStatus() != KnowledgeItemStatus.DRAFT) {
            throw new BusinessException(
                    String.format("Bilgi öğesi READY yapılamaz. Mevcut statü: %s, Beklenen: DRAFT",
                            entity.getStatus()));
        }

        entity.setStatus(KnowledgeItemStatus.READY);
        KnowledgeItem saved = knowledgeItemRepository.save(entity);
        log.info("Bilgi öğesi READY yapıldı: id={}", saved.getId());

        return toResult(saved);
    }

    // ── Archive ─────────────────────────────────────────────

    /**
     * Bilgi öğesini ARCHIVED statüsüne geçirir.
     * DRAFT → ARCHIVED ve READY → ARCHIVED geçişleri geçerlidir.
     * ARCHIVED → ARCHIVED tekrar çağrıldığında hata fırlatılır.
     */
    @Transactional
    public KnowledgeItemResult archive(UUID id) {
        KnowledgeItem entity = findOrThrow(id);

        if (entity.getStatus() == KnowledgeItemStatus.ARCHIVED) {
            throw new BusinessException("Bilgi öğesi zaten arşivlenmiş: " + id);
        }

        entity.setStatus(KnowledgeItemStatus.ARCHIVED);
        KnowledgeItem saved = knowledgeItemRepository.save(entity);
        log.info("Bilgi öğesi arşivlendi: id={}", saved.getId());

        return toResult(saved);
    }

    // ── Private Helpers ─────────────────────────────────────

    private KnowledgeItem findOrThrow(UUID id) {
        return knowledgeItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("KnowledgeItem", id));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Başlık boş olamaz");
        }
        if (title.length() > 255) {
            throw new ValidationException("Başlık en fazla 255 karakter olabilir");
        }
    }

    private void validateSlugUniqueness(String title, UUID excludeId) {
        String slug = generateSlug(title);
        knowledgeItemRepository.findBySlug(slug).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new ValidationException("Bu başlığa sahip bir bilgi öğesi zaten mevcut: " + title);
            }
        });
    }

    private KnowledgeItemType resolveType(String type) {
        if (type == null || type.isBlank()) {
            throw new ValidationException("Tür boş olamaz");
        }
        try {
            return KnowledgeItemType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Geçersiz bilgi öğesi türü: " + type);
        }
    }

    private KnowledgeItemResult toResult(KnowledgeItem entity) {
        return KnowledgeItemResult.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .locationText(entity.getLocationText())
                .notes(entity.getNotes())
                .status(entity.getStatus().name())
                .language(entity.getLanguage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Başlıktan URL-uyumlu slug üretir.
     * Türkçe karakterler dönüştürülür, boşluklar tire ile değiştirilir.
     */
    static String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        return title.toLowerCase()
                .replace("ç", "c").replace("ğ", "g").replace("ı", "i")
                .replace("ö", "o").replace("ş", "s").replace("ü", "u")
                .replace("Ç", "c").replace("Ğ", "g").replace("İ", "i")
                .replace("Ö", "o").replace("Ş", "s").replace("Ü", "u")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
