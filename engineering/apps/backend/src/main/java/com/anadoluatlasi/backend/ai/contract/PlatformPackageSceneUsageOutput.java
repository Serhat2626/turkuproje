package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

/**
 * FP-004: Platform paketinde sahne kullanım çıktısı.
 */
@Value
@Builder
public class PlatformPackageSceneUsageOutput {

    /** Storyboard sahne numarası */
    int sceneNumber;

    /** Kullanım tipi (PRIMARY, ADAPTED, PARTIAL, OMITTED) */
    String usageType;

    /** Bu pakette kullanılıyor mu */
    boolean included;

    /** Adaptasyon notu */
    String adaptationNote;
}
