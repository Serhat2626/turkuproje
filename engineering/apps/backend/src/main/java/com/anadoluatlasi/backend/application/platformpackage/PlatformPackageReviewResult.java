package com.anadoluatlasi.backend.application.platformpackage;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-004: Platform paketi review operasyonlarının sonuç nesnesi.
 * Edit, approve, reject işlemlerinin ortak dönüş tipi.
 * FP-003 StoryboardReviewResult pattern'ı ile tutarlıdır.
 */
@Value
@Builder
public class PlatformPackageReviewResult {

    /** İşlem başarılı mı */
    boolean success;

    /** PlatformPackageDraft ID */
    UUID packageDraftId;

    /** Content Project ID */
    UUID contentProjectId;

    /** Güncel status */
    String status;

    /** Platform tipi */
    String packageType;

    /** Paket başlığı */
    String title;

    /** Sonuç mesajı */
    String message;
}
