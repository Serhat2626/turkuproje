package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

/**
 * FP-004: Kaynak storyboard bilgisi input contract'ı.
 */
@Value
@Builder
public class PlatformPackageSourceStoryboardInput {

    /** Storyboard başlığı */
    String title;

    /** Storyboard özeti */
    String summary;

    /** Görsel stil rehberi */
    String visualStyleGuide;

    /** Anlatım stili */
    String narrationStyle;

    /** Hedef platform (kaynak) */
    String targetPlatform;

    /** Aspect ratio (kaynak) */
    String aspectRatio;

    /** Tahmini toplam süre (saniye) */
    Integer estimatedDurationSeconds;

    /** Toplam sahne sayısı */
    Integer totalSceneCount;
}
