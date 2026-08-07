package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TurkuVideoProjectRepository implementasyonu — JPA ile PostgreSQL'e kalıcı kayıt.
 * Domain object (TurkuVideoProject) ↔ JPA Entity (JpaTurkuVideoProjectEntity) dönüşümü yapar.
 * Complex alanlar Jackson ile JSONB olarak serialize/deserialize edilir.
 */
@Repository
@Primary
public class JpaTurkuVideoProjectRepositoryAdapter implements TurkuVideoProjectRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaTurkuVideoProjectRepositoryAdapter.class);

    private final JpaTurkuVideoProjectJpaRepository jpaRepo;
    private final ObjectMapper objectMapper;

    public JpaTurkuVideoProjectRepositoryAdapter(JpaTurkuVideoProjectJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
        this.objectMapper = createObjectMapper();
    }

    /**
     * ObjectMapper yapılandırması — domain sınıflarına annotation eklemeden
     * Builder pattern'ı desteklemek için mixin'ler kullanır.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // Field visibility: private alanları da serialize/deserialize et
        mapper.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.FIELD, 
                             com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.GETTER, 
                             com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);
        
        return mapper;
    }

    @Override
    public TurkuVideoProject save(TurkuVideoProject project) {
        JpaTurkuVideoProjectEntity entity = toEntity(project);
        jpaRepo.save(entity);
        log.debug("💾 Proje DB'ye kaydedildi: id={}, stage={}", project.getId(), project.getWorkflowStage());
        return project;
    }

    @Override
    public Optional<TurkuVideoProject> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<TurkuVideoProject> findAll() {
        return jpaRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepo.count();
    }

    @Override
    public void deleteAll() {
        jpaRepo.deleteAll();
    }

    @Override
    public List<TurkuVideoProject> findByWorkflowStage(TurkuVideoWorkflowStage workflowStage) {
        return jpaRepo.findByWorkflowStage(workflowStage.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TurkuVideoProject> findByNeedsHumanReviewTrue() {
        return jpaRepo.findByNeedsHumanReviewTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TurkuVideoProject> findByTurkuNameContainingIgnoreCase(String turkuName) {
        return jpaRepo.findByTurkuNameContainingIgnoreCase(turkuName).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TurkuVideoProject> findBySourceConfidence(TurkuSourceConfidence sourceConfidence) {
        return jpaRepo.findBySourceConfidence(sourceConfidence.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════
    // Domain → Entity
    // ══════════════════════════════════════════════════════════════

    private JpaTurkuVideoProjectEntity toEntity(TurkuVideoProject p) {
        JpaTurkuVideoProjectEntity e = new JpaTurkuVideoProjectEntity();
        e.setId(p.getId());
        e.setTurkuName(p.getTurkuName());
        e.setRegion(p.getRegion());
        e.setSourceNotes(p.getSourceNotes());
        e.setMood(p.getMood());
        e.setTargetDurationMinutes(p.getTargetDurationMinutes());
        e.setNarrationStyle(p.getNarrationStyle());
        e.setVisualStyle(p.getVisualStyle());
        e.setWorkflowStage(p.getWorkflowStage().name());
        e.setSourceConfidence(p.getSourceConfidence() != null ? p.getSourceConfidence().name() : "UNKNOWN");
        e.setNeedsHumanReview(p.isNeedsHumanReview());
        e.setCopyrightReviewRequired(p.isCopyrightReviewRequired());
        e.setCreatedByActorReference(p.getCreatedByActorReference());
        e.setProviderMode(p.getProviderMode() != null ? p.getProviderMode().name() : "DETERMINISTIC_STUB");
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());

        // Complex alanlar → JSONB
        e.setStoryPackageJson(toJson(p.getStoryPackage()));
        e.setScenarioPlanJson(toJson(p.getScenarioPlan()));
        e.setStoryboardPlanJson(toJson(p.getStoryboardPlan()));
        e.setSceneAssetPlanJson(toJson(p.getSceneAssetPlan()));
        e.setImageBatchJson(toJson(p.getImageBatch()));
        e.setCompositionPlanJson(toJson(p.getCompositionPlan()));
        e.setRenderManifestJson(toJson(p.getRenderManifest()));
        e.setRenderJobJson(toJson(p.getRenderJob()));

        return e;
    }

    // ══════════════════════════════════════════════════════════════
    // Entity → Domain
    // ══════════════════════════════════════════════════════════════

    private TurkuVideoProject toDomain(JpaTurkuVideoProjectEntity e) {
        TurkuVideoProject p = TurkuVideoProject.builder()
                .id(e.getId())
                .turkuName(e.getTurkuName())
                .region(e.getRegion())
                .sourceNotes(e.getSourceNotes())
                .mood(e.getMood())
                .targetDurationMinutes(e.getTargetDurationMinutes())
                .narrationStyle(e.getNarrationStyle())
                .visualStyle(e.getVisualStyle())
                .workflowStage(safeEnum(TurkuVideoWorkflowStage.class, e.getWorkflowStage(), TurkuVideoWorkflowStage.DRAFT_CREATED))
                .sourceConfidence(safeEnum(TurkuSourceConfidence.class, e.getSourceConfidence(), TurkuSourceConfidence.UNKNOWN))
                .needsHumanReview(e.isNeedsHumanReview())
                .copyrightReviewRequired(e.isCopyrightReviewRequired())
                .createdByActorReference(e.getCreatedByActorReference())
                .createdAt(e.getCreatedAt())
                .build();

        // Provider mode
        if (e.getProviderMode() != null) {
            p.setProviderMode(safeEnum(TurkuProviderMode.class, e.getProviderMode(), TurkuProviderMode.DETERMINISTIC_STUB));
        }

        // Stage'i doğru set et (builder default'u override edebilir)
        TurkuVideoWorkflowStage stage = safeEnum(TurkuVideoWorkflowStage.class, e.getWorkflowStage(), TurkuVideoWorkflowStage.DRAFT_CREATED);
        p.advanceTo(stage);

        // Complex alanlar ← JSONB
        p.setStoryPackage(fromJson(e.getStoryPackageJson(), TurkuStoryPackage.class));
        p.setScenarioPlan(fromJson(e.getScenarioPlanJson(), TurkuScenarioPlan.class));
        p.setStoryboardPlan(fromJson(e.getStoryboardPlanJson(), TurkuStoryboardPlan.class));
        p.setSceneAssetPlan(fromJson(e.getSceneAssetPlanJson(), TurkuSceneAssetPlan.class));
        p.setImageBatch(fromJson(e.getImageBatchJson(), TurkuSceneImageBatch.class));
        p.setCompositionPlan(fromJson(e.getCompositionPlanJson(), TurkuVideoCompositionPlan.class));
        p.setRenderManifest(fromJson(e.getRenderManifestJson(), TurkuRenderManifest.class));
        p.setRenderJob(fromJson(e.getRenderJobJson(), TurkuRenderJob.class));

        return p;
    }

    // ══════════════════════════════════════════════════════════════
    // JSON helpers
    // ══════════════════════════════════════════════════════════════

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            log.warn("JSONB serialize hatası: {}", ex.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            log.warn("JSONB deserialize hatası ({}): {}", type.getSimpleName(), ex.getMessage());
            return null;
        }
    }

    private <E extends Enum<E>> E safeEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex) {
            log.warn("Bilinmeyen enum değeri: {} → {}", enumClass.getSimpleName(), value);
            return defaultValue;
        }
    }
}
