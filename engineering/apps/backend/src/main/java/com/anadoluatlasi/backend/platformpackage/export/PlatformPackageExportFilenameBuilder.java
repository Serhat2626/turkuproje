package com.anadoluatlasi.backend.platformpackage.export;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * FP-005: Export dosya adı üretici.
 *
 * <p>Standart format: {@code {slug}_{packageType}_{version}.{ext}}</p>
 * <p>Örnek: {@code truva-atinin-hikayesi_youtube-long-form_v1.md}</p>
 *
 * <p>Slug üretimi Unicode-safe'dir ve Türkçe karakterleri ASCII'ye dönüştürür.</p>
 */
public final class PlatformPackageExportFilenameBuilder {

    // Ön-derlenen regex patternleri
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-{2,}");

    private PlatformPackageExportFilenameBuilder() {
        // Utility class — instantiation yok
    }

    /**
     * Export dosya adı üretir.
     *
     * @param title       İçerik başlığı (ör. "Truva Atı'nın Hikayesi")
     * @param packageType Platform tipi enum adı (ör. "YOUTUBE_LONG_FORM")
     * @param version     Paket versiyonu
     * @param exportType  Export tipi (dosya uzantısını belirler)
     * @return Standart dosya adı (ör. "truva-atinin-hikayesi_youtube-long-form_v1.md")
     */
    public static String build(String title, String packageType,
                               int version, PlatformPackageExportType exportType) {
        String slug = slugify(title);
        String type = packageType.toLowerCase(Locale.ENGLISH).replace('_', '-');
        return slug + "_" + type + "_v" + version + "." + exportType.getFileExtension();
    }

    /**
     * Başlık metnini URL/dosya-adı dostu slug'a dönüştürür.
     *
     * <p>Türkçe karakter dönüşümleri: ç→c, ğ→g, ı→i, ö→o, ş→s, ü→u</p>
     */
    static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "export";
        }

        String result = input.toLowerCase(Locale.forLanguageTag("tr"));

        // Türkçe özel karakter dönüşümleri (NFD decomposition öncesinde)
        result = result
                .replace('ç', 'c')
                .replace('ğ', 'g')
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ş', 's')
                .replace('ü', 'u');

        // Unicode normalize — aksanlı karakterleri base + combining'e ayır
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        // Combining marks'ı kaldır
        result = result.replaceAll("\\p{M}", "");

        // Boşlukları tire'ye çevir
        result = result.replace(' ', '-');
        // Alfanumerik ve tire harici her şeyi kaldır
        result = NON_ALPHANUMERIC.matcher(result).replaceAll("");
        // Ardışık tireleri teke düşür
        result = MULTI_HYPHEN.matcher(result).replaceAll("-");
        // Baştaki ve sondaki tireleri kaldır
        result = result.replaceAll("^-|-$", "");

        return result.isEmpty() ? "export" : result;
    }
}
