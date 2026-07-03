package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-004: Kaynak storyboard sahnesi input contract'ı.
 */
@Value
@Builder
public class PlatformPackageSourceSceneInput {

    /** Sahne numarası */
    int sceneNumber;

    /** Sahne ID (opsiyonel, referans için) */
    UUID sceneId;

    /** Sahne başlığı */
    String sceneTitle;

    /** Görsel açıklama */
    String visualDescription;

    /** Anlatım metni */
    String narrationText;

    /** Mood */
    String mood;

    /** Tahmini süre (saniye) */
    int estimatedDurationSeconds;

    /** Kaynak beat tipi (HOOK, INTRODUCTION vb.) */
    String sourceBeatType;
}
