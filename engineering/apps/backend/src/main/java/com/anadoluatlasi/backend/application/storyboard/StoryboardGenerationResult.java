package com.anadoluatlasi.backend.application.storyboard;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-003: Storyboard üretim operasyonlarının sonuç nesnesi.
 * Başarı ve hata durumlarını tek tip olarak taşır.
 * FP-002 ScenarioGenerationResult pattern'ı ile tutarlıdır.
 */
@Value
@Builder
public class StoryboardGenerationResult {

    /** İşlem başarılı mı */
    boolean success;

    /** Content Project ID */
    UUID contentProjectId;

    /** Oluşturulan StoryboardDraft ID */
    UUID storyboardDraftId;

    /** Storyboard version numarası */
    Integer version;

    /** Storyboard güncel status'ü */
    String status;

    /** Storyboard başlığı (başarılıysa) */
    String storyboardTitle;

    /** Scene sayısı (başarılıysa) */
    Integer sceneCount;

    /** Sonuç mesajı */
    String message;

    /** Hata kodu (başarısızsa) */
    String errorCode;
}
