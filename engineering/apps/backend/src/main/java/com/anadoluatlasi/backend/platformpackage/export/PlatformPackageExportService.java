package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.repository.PlatformPackageDraftRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FP-005: Platform paketi deterministik export service.
 *
 * <p>APPROVED PlatformPackageDraft verilerinden formatlanmış export çıktıları üretir.
 * Read-only, side-effect free. AI çağrısı yapmaz. DB'ye yazmaz. Status değiştirmez.</p>
 *
 * <p>Governance kuralları:</p>
 * <ul>
 *   <li>Sadece APPROVED paketler export edilebilir</li>
 *   <li>AI_GENERATED, NEEDS_REVIEW, REJECTED, FAILED → exception</li>
 *   <li>Export, status mutasyonu yapmaz</li>
 *   <li>Export, READY_FOR_PUBLISH atamaz</li>
 *   <li>Export, raw prompt/provider payload/API key/secret expose etmez</li>
 * </ul>
 */
@Slf4j
@Service
public class PlatformPackageExportService {

    private final PlatformPackageDraftRepository repository;
    private final Map<PlatformPackageExportType, PlatformPackageExportFormatter> formatters;

    /**
     * Spring, tüm PlatformPackageExportFormatter bean'lerini otomatik enjekte eder.
     */
    public PlatformPackageExportService(
            PlatformPackageDraftRepository repository,
            List<PlatformPackageExportFormatter> formatterList) {
        this.repository = repository;
        this.formatters = formatterList.stream()
                .collect(Collectors.toMap(
                        PlatformPackageExportFormatter::supports,
                        Function.identity()
                ));
    }

    /**
     * Onaylanmış bir platform paketini belirtilen formatta export eder.
     *
     * <p>Read-only işlem — DB'ye yazmaz, entity mutasyonu yapmaz.</p>
     *
     * @param packageId  Export edilecek paketin kimliği
     * @param exportType Export format tipi
     * @return Formatted export payload
     * @throws NotFoundException                              Package bulunamadı (404)
     * @throws PlatformPackageExportNotAllowedException        Package APPROVED değil (409)
     * @throws UnsupportedPlatformPackageExportTypeException   Geçersiz export tipi (400)
     */
    @Transactional(readOnly = true)
    public PlatformPackageExportResponse exportPackage(UUID packageId,
                                                        PlatformPackageExportType exportType) {
        // 1. Validate export type
        validateExportType(exportType);

        // 2. Load package (with lazy loads resolved)
        PlatformPackageDraft draft = loadAndValidatePackage(packageId);

        // 3. Build safe metadata
        PlatformPackageExportMetadata metadata = PlatformPackageExportMetadataMapper.mapFrom(draft);

        // 4. Format content
        PlatformPackageExportFormatter formatter = formatters.get(exportType);
        String content = formatter.format(draft, metadata);

        // 5. Build filename
        String filename = PlatformPackageExportFilenameBuilder.build(
                draft.getTitle(),
                draft.getPackageType().name(),
                draft.getVersion(),
                exportType
        );

        log.info("Platform paketi export edildi: packageId={}, exportType={}, filename={}",
                packageId, exportType, filename);

        // 6. Build response
        return PlatformPackageExportResponse.builder()
                .packageId(packageId)
                .packageType(draft.getPackageType().name())
                .exportType(exportType)
                .filename(filename)
                .contentType(exportType.getContentType())
                .content(content)
                .exportedAt(Instant.now())
                .metadata(metadata)
                .build();
    }

    // ── Private helpers ──

    private PlatformPackageDraft loadAndValidatePackage(UUID packageId) {
        // Load
        PlatformPackageDraft draft = repository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("PlatformPackageDraft", packageId));

        // Force lazy loads (open-in-view=false)
        draft.getSceneUsages().size();
        draft.getContentProject().getId();
        draft.getSourceStoryboardDraft().getId();

        // Validate APPROVED
        if (draft.getStatus() != PlatformPackageStatus.APPROVED) {
            throw new PlatformPackageExportNotAllowedException(packageId, draft.getStatus().name());
        }

        return draft;
    }

    private void validateExportType(PlatformPackageExportType exportType) {
        if (exportType == null) {
            throw new UnsupportedPlatformPackageExportTypeException("null");
        }
        if (!formatters.containsKey(exportType)) {
            throw new UnsupportedPlatformPackageExportTypeException(exportType.name());
        }
    }
}
