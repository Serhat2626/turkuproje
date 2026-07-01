package com.anadoluatlasi.backend.scenario.entity;

/**
 * FP-002: Senaryo beat tiplerini tanımlayan enum.
 *
 * <p>Video senaryosunun 6 temel anlatım birimini temsil eder.
 * Sıralama sabittir ve provider-agnostic'tir.</p>
 *
 * <p>Sıra: Hook → Giriş → Hikâye → Duygusal Zirve → Kapanış → CTA</p>
 */
public enum ScenarioBeatType {

    /** Açılış hook'u — ilk 5-10 saniye, izleyiciyi yakala */
    HOOK(1, "Hook"),

    /** Giriş — konunun tanıtımı */
    INTRODUCTION(2, "Giriş"),

    /** Ana anlatım bloğu — video süresinin ~%60'ı */
    MAIN_NARRATIVE(3, "Hikâye"),

    /** Duygusal zirve — en etkili/duygusal an */
    EMOTIONAL_CLIMAX(4, "Duygusal Zirve"),

    /** Kapanış — sonuç, tarihî önem, ders */
    CONCLUSION(5, "Kapanış"),

    /** İzleyici eylemi çağrısı — abone ol, yorum yap, paylaş */
    CALL_TO_ACTION(6, "CTA");

    private final int order;
    private final String label;

    ScenarioBeatType(int order, String label) {
        this.order = order;
        this.label = label;
    }

    public int getOrder() {
        return order;
    }

    public String getLabel() {
        return label;
    }
}
