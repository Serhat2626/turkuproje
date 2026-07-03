package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageSceneUsage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FP-005: Safe metadata mapper.
 *
 * <p>PlatformPackageDraft ve ilişkili entity'lerden yalnızca safe alanları okur.
 * Raw prompt, provider payload, API key veya secret asla dahil edilmez.</p>
 *
 * <p>Entity'de zaten rawPrompt/providerPayload alanı bulunmaz — bu mapper
 * sadece mevcut safe alanlardan export metadata üretir.</p>
 */
public final class PlatformPackageExportMetadataMapper {

    private PlatformPackageExportMetadataMapper() {
        // Utility class
    }

    /**
     * PlatformPackageDraft'tan safe export metadata üretir.
     *
     * @param draft APPROVED PlatformPackageDraft (lazy loads resolved)
     * @return Safe metadata DTO
     */
    public static PlatformPackageExportMetadata mapFrom(PlatformPackageDraft draft) {
        return PlatformPackageExportMetadata.builder()
                .contentProjectId(draft.getContentProject().getId())
                .sourceStoryboardDraftId(draft.getSourceStoryboardDraft().getId())
                .version(draft.getVersion())
                .packageStatus(draft.getStatus().name())
                .packageTitle(draft.getTitle())
                .approvedAt(draft.getApprovedAt())
                .approvedBy(draft.getApprovedBy())
                .providerType(draft.getProviderType())
                .modelName(draft.getModelName())
                .generatedBy(draft.getGeneratedBy() != null ? draft.getGeneratedBy().name() : null)
                .sceneUsages(mapSceneUsages(draft.getSceneUsages()))
                .build();
    }

    private static List<PlatformPackageExportSceneUsageDto> mapSceneUsages(
            List<PlatformPackageSceneUsage> usages) {
        if (usages == null || usages.isEmpty()) {
            return List.of();
        }
        return usages.stream()
                .map(u -> PlatformPackageExportSceneUsageDto.builder()
                        .sourceStoryboardSceneId(
                                u.getSourceStoryboardScene() != null
                                        ? u.getSourceStoryboardScene().getId()
                                        : null)
                        .sceneNumber(u.getSceneNumber())
                        .usageType(u.getUsageType())
                        .included(u.getIncluded())
                        .adaptationNote(u.getAdaptationNote())
                        .build())
                .collect(Collectors.toList());
    }
}
