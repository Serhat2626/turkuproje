package com.anadoluatlasi.backend.platformpackage.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-005: Export çıktısında sahne kullanım bilgilerini taşıyan DTO.
 *
 * <p>PlatformPackageSceneUsage entity'sinden türetilir.
 * Yalnızca safe alanlar dahil edilir.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageExportSceneUsageDto {

    /** Kaynak storyboard sahne kimliği (nullable — tüm sahnelerin referansı olmayabilir) */
    private UUID sourceStoryboardSceneId;

    /** Sahne sıra numarası */
    private Integer sceneNumber;

    /** Kullanım tipi (PRIMARY, ADAPTED, PARTIAL, OMITTED) */
    private String usageType;

    /** Bu pakette kullanılıyor mu */
    private Boolean included;

    /** Adaptasyon / production notu */
    private String adaptationNote;
}
