package com.anadoluatlasi.backend.platformpackage.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * FP-005: Platform paketi export yanıt DTO'su.
 *
 * <p>Onaylanmış (APPROVED) PlatformPackageDraft verilerinden üretilen
 * deterministik export çıktısını taşır. Raw prompt, provider payload,
 * API key veya secret içermez.</p>
 *
 * <p>Export, DB'ye yazılmaz — on-demand üretilir ve HTTP response olarak döner.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageExportResponse {

    /** Export edilen paketin kimliği */
    private UUID packageId;

    /** Platform tipi (YOUTUBE_LONG_FORM, YOUTUBE_SHORTS, vb.) */
    private String packageType;

    /** Export format tipi */
    private PlatformPackageExportType exportType;

    /** Önerilen indirme dosya adı */
    private String filename;

    /** HTTP Content-Type değeri */
    private String contentType;

    /** Formatted export içeriği (markdown, json, text) */
    private String content;

    /** Export üretim zaman damgası */
    private Instant exportedAt;

    /** Export kaynak ve metadata bilgileri */
    private PlatformPackageExportMetadata metadata;
}
