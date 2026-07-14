package com.anadoluatlasi.backend.turkuvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-020 Task 02: Safe image generation request.
 *
 * <p>Hikâye bağlamı, karakter tanımı ve sahne akışı bilgileri içerir.
 * Her görsel, hikâyenin bir karesini oluşturur — tutarlılık için tüm bağlam verilir.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurkuImageGenerationRequest {

    private UUID projectId;
    private int sceneNo;
    private int totalScenes;
    private String safePromptSummary;
    private String safeNegativeNotes;
    private String styleHint;
    private String aspectRatioHint;

    /** Hikâye özeti — tüm görsellerde tutarlılık için */
    private String storyContext;

    /** Karakter fiziksel tanımları — tüm sahnelerde aynı karakter */
    private String characterDescription;

    /** Önceki sahnenin görsel açıklaması — akış sağlar */
    private String previousSceneVisual;

    /** Sahne mood/atmosfer — ışık ve renk yönlendirmesi */
    private String sceneMood;
}
