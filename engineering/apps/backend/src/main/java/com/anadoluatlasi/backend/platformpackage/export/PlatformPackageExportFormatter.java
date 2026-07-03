package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;

/**
 * FP-005: Platform paketi export formatter interface'i.
 *
 * <p>Her export tipi için bir implementasyon bulunur. Formatter'lar deterministik,
 * read-only ve side-effect free'dir. AI çağrısı, DB write veya harici IO yapmaz.</p>
 *
 * <p>Implementasyonlar:</p>
 * <ul>
 *   <li>{@code MarkdownBriefPlatformPackageExportFormatter} — MARKDOWN_BRIEF</li>
 *   <li>{@code JsonPlatformPackageExportFormatter} — JSON_PACKAGE</li>
 *   <li>{@code CopyTextPlatformPackageExportFormatter} — COPY_TEXT</li>
 *   <li>{@code ProductionBriefPlatformPackageExportFormatter} — PRODUCTION_BRIEF</li>
 * </ul>
 */
public interface PlatformPackageExportFormatter {

    /**
     * Bu formatter'ın desteklediği export tipini döndürür.
     */
    PlatformPackageExportType supports();

    /**
     * PlatformPackageDraft verilerinden formatted export içeriği üretir.
     *
     * @param draft    APPROVED PlatformPackageDraft (scene usages yüklenmiş)
     * @param metadata Safe export metadata (source traceability, approval info)
     * @return Formatted export string (markdown, json, text)
     */
    String format(PlatformPackageDraft draft, PlatformPackageExportMetadata metadata);
}
