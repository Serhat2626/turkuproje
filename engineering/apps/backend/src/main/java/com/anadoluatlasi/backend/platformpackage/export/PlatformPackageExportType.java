package com.anadoluatlasi.backend.platformpackage.export;

/**
 * FP-005: Platform paketi export tiplerini belirten enum.
 *
 * <p>İlk faz export tipleri — deterministik formatlama, AI çağrısı gerekmez.</p>
 * <ul>
 *   <li>{@code MARKDOWN_BRIEF} — İnsan-okunabilir markdown çıktı</li>
 *   <li>{@code JSON_PACKAGE} — Yapılandırılmış JSON çıktı</li>
 *   <li>{@code COPY_TEXT} — Clipboard-optimized düz metin</li>
 *   <li>{@code PRODUCTION_BRIEF} — Editör/tasarımcı handoff brief (markdown)</li>
 * </ul>
 *
 * <p>Export, APPROVED PlatformPackageDraft verilerinden üretilir.
 * Raw prompt, provider payload, API key veya secret export çıktısında yer almaz.</p>
 */
public enum PlatformPackageExportType {

    /** İnsan-okunabilir, bölümlere ayrılmış markdown çıktı */
    MARKDOWN_BRIEF("text/markdown", "md"),

    /** Yapılandırılmış, makine-okunabilir JSON çıktı */
    JSON_PACKAGE("application/json", "json"),

    /** Clipboard kullanımı için optimize edilmiş düz metin */
    COPY_TEXT("text/plain", "txt"),

    /** Editör/tasarımcı/video editörü için production handoff brief (markdown) */
    PRODUCTION_BRIEF("text/markdown", "md");

    private final String contentType;
    private final String fileExtension;

    PlatformPackageExportType(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    /**
     * HTTP Content-Type header değerini döndürür.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * İndirilebilir dosya uzantısını döndürür (nokta hariç).
     */
    public String getFileExtension() {
        return fileExtension;
    }
}
