package com.anadoluatlasi.backend.application.platformpackage;

import lombok.Builder;
import lombok.Value;

/**
 * FP-004: Platform paketi sahne kullanım düzenleme komut nesnesi.
 */
@Value
@Builder
public class PlatformPackageSceneUsageEditCommand {

    /** Sahne numarası (zorunlu — hangi scene'i düzenleyeceğimizi belirler) */
    int sceneNumber;

    /** Kullanım tipi (PRIMARY, ADAPTED, PARTIAL, OMITTED) */
    String usageType;

    /** Bu pakette kullanılıyor mu */
    Boolean included;

    /** Adaptasyon notu */
    String adaptationNote;
}
