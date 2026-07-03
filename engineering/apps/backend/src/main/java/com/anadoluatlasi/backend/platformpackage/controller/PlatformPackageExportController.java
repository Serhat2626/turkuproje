package com.anadoluatlasi.backend.platformpackage.controller;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportResponse;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportService;
import com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportType;
import com.anadoluatlasi.backend.platformpackage.export.UnsupportedPlatformPackageExportTypeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * FP-005: Admin Platform Package Export REST controller.
 *
 * <p>Thin controller — sadece export service çağırır.
 * İş kuralı, repository veya entity logic içermez.
 * Read-only, side-effect free. DB'ye yazmaz. Status değiştirmez.</p>
 *
 * <p>Governance kuralları:</p>
 * <ul>
 *   <li>Sadece APPROVED paketler export edilebilir</li>
 *   <li>Export, raw prompt/provider payload/API key/secret expose etmez</li>
 *   <li>Export, READY_FOR_PUBLISH atamaz</li>
 *   <li>Export, publish/upload/schedule yapmaz</li>
 *   <li>Export, media generate etmez</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Platform Package Export", description = "Platform paketi export aksiyonları (Admin)")
public class PlatformPackageExportController {

    private final PlatformPackageExportService exportService;

    /**
     * GET /api/admin/platform-packages/{packageId}/exports/{exportType}
     *
     * <p>Onaylanmış bir platform paketini belirtilen formatta export eder.</p>
     *
     * <p>Desteklenen exportType değerleri:</p>
     * <ul>
     *   <li>MARKDOWN_BRIEF — text/markdown</li>
     *   <li>JSON_PACKAGE — application/json</li>
     *   <li>COPY_TEXT — text/plain</li>
     *   <li>PRODUCTION_BRIEF — text/markdown</li>
     * </ul>
     *
     * @param packageId  Export edilecek paketin UUID'si
     * @param exportType Export format tipi (PlatformPackageExportType enum)
     * @return Formatted export response
     *
     * @throws com.anadoluatlasi.backend.common.exception.NotFoundException
     *         Paket bulunamazsa (HTTP 404)
     * @throws com.anadoluatlasi.backend.platformpackage.export.PlatformPackageExportNotAllowedException
     *         Paket APPROVED değilse (HTTP 409)
     * @throws UnsupportedPlatformPackageExportTypeException
     *         Geçersiz export tipi (HTTP 400)
     */
    @GetMapping("/platform-packages/{packageId}/exports/{exportType}")
    @Operation(summary = "Platform paketini export et",
            description = "Onaylanmış platform paketini belirtilen formatta deterministik olarak export eder")
    public ResponseEntity<ApiResponse<PlatformPackageExportResponse>> exportPackage(
            @PathVariable UUID packageId,
            @PathVariable String exportType) {

        // String → Enum conversion (path variable String gelir)
        PlatformPackageExportType type = parseExportType(exportType);

        PlatformPackageExportResponse response = exportService.exportPackage(packageId, type);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Path variable string'ini PlatformPackageExportType enum'una parse eder.
     * Geçersiz değer için UnsupportedPlatformPackageExportTypeException fırlatır.
     */
    private PlatformPackageExportType parseExportType(String exportType) {
        if (exportType == null || exportType.isBlank()) {
            throw new UnsupportedPlatformPackageExportTypeException("empty");
        }
        try {
            return PlatformPackageExportType.valueOf(exportType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnsupportedPlatformPackageExportTypeException(exportType);
        }
    }
}
