package com.anadoluatlasi.backend.visualasset.entity;

/**
 * FP-006: Visual Asset Plan lifecycle status.
 *
 * <p>Görsel varlık planının yaşam döngüsü durumunu belirler.
 * FP-003 StoryboardDraftStatus ve FP-004 PlatformPackageStatus ile aynı pattern.</p>
 *
 * <p>Kurallar:</p>
 * <ul>
 *   <li>AI yalnızca AI_GENERATED veya FAILED üretebilir.</li>
 *   <li>APPROVED açık admin eylemi gerektirir.</li>
 *   <li>READY_FOR_PUBLISH bu enum'da yoktur.</li>
 * </ul>
 */
public enum VisualAssetPlanStatus {
    AI_GENERATED,
    NEEDS_REVIEW,
    APPROVED,
    REJECTED,
    FAILED
}
