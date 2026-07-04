package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

/**
 * FP-006: Thumbnail üretim context input'u.
 *
 * <p>Video thumbnail için görsel planlama bağlamı.</p>
 */
@Value
@Builder
public class VisualAssetThumbnailInput {

    /** Video/içerik başlığı */
    String title;

    /** Hikâyenin dikkat çekici öğesi */
    String storyHook;

    /** Tercih edilen aspect ratio */
    String preferredAspectRatio;

    /** Duygusal ton (merak, nostalji, heyecan, etc.) */
    String emotionalTone;

    /** Metin overlay politikası */
    String textOverlayPolicy;
}
