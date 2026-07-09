package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * LIVE-005: Human review checklist item.
 */
public final class TurkuLiveReviewChecklistItem {
    private final String label;
    private final boolean checked;
    private final String note;

    public TurkuLiveReviewChecklistItem(String label, boolean checked, String note) {
        this.label = label;
        this.checked = checked;
        this.note = note;
    }

    public String getLabel() { return label; }
    public boolean isChecked() { return checked; }
    public String getNote() { return note; }

    public static List<TurkuLiveReviewChecklistItem> defaultChecklist() {
        return List.of(
                new TurkuLiveReviewChecklistItem("Gorsel kalitesi kabul edilebilir", false, ""),
                new TurkuLiveReviewChecklistItem("Ses/muzik uyumlu", false, ""),
                new TurkuLiveReviewChecklistItem("Metin/altyazi dogru", false, ""),
                new TurkuLiveReviewChecklistItem("Telif ihlali yok", false, ""),
                new TurkuLiveReviewChecklistItem("Icerik politikasina uygun", false, ""),
                new TurkuLiveReviewChecklistItem("Teknik format gecerli", false, ""));
    }
}
