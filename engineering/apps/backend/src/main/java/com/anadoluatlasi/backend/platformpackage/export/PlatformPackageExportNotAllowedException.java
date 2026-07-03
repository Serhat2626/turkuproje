package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.common.exception.BusinessException;

import java.util.UUID;

/**
 * FP-005: Onaylanmamış (non-APPROVED) paketten export denemesinde fırlatılır.
 *
 * <p>GlobalExceptionHandler tarafından HTTP 409 Conflict olarak döndürülür.</p>
 *
 * <p>Sadece APPROVED PlatformPackageDraft export edilebilir.
 * AI_GENERATED, NEEDS_REVIEW, REJECTED, FAILED statuslar reddedilir.</p>
 */
public class PlatformPackageExportNotAllowedException extends BusinessException {

    public PlatformPackageExportNotAllowedException(UUID packageId, String currentStatus) {
        super(String.format(
                "Sadece APPROVED paketler export edilebilir. " +
                "Paket ID: %s, mevcut durum: %s",
                packageId, currentStatus
        ));
    }
}
