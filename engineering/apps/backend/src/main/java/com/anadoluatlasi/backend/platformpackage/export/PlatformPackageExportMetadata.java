package com.anadoluatlasi.backend.platformpackage.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-005: Export kaynak bilgileri ve traceability metadata DTO'su.
 *
 * <p>Export çıktısında yer alan source referansları, onay bilgileri ve
 * sahne kullanım özetini taşır. Yalnızca safe alanlar dahil edilir.</p>
 *
 * <p>Yasak alanlar: rawPrompt, providerPayload, apiKey, secret.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageExportMetadata {

    // ── Source Traceability ──

    /** Kaynak content project kimliği */
    private UUID contentProjectId;

    /** Kaynak storyboard draft kimliği */
    private UUID sourceStoryboardDraftId;

    /** Paket versiyonu */
    private Integer version;

    /** Paket durumu (APPROVED olmalı) */
    private String packageStatus;

    /** Paket başlığı */
    private String packageTitle;

    // ── Approval Info ──

    /** Onay zaman damgası */
    private LocalDateTime approvedAt;

    /** Onaylayan admin kimliği */
    private String approvedBy;

    // ── Safe AI Metadata ──

    /** Provider tipi (OPENAI, MOCK) */
    private String providerType;

    /** Model adı (gpt-4o-mini) */
    private String modelName;

    /** Üretim kaynağı (AI_CONTENT_AGENT) */
    private String generatedBy;

    // ── Scene Usages ──

    /** Sahne kullanım özetleri */
    private List<PlatformPackageExportSceneUsageDto> sceneUsages;
}
