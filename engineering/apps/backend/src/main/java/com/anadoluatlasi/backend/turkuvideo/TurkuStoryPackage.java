package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

/**
 * FP-019 Task 03: Türkü story package — AI-researched cultural/historical context.
 *
 * <p>Does not store lyrics, copyrighted media, public URLs, raw provider payloads,
 * or credential/session data.</p>
 */
@JsonDeserialize(builder = TurkuStoryPackage.Builder.class)
public final class TurkuStoryPackage {

    private final String turkuName;
    private final String regionContext;
    private final String originSummary;
    private final String emotionalTheme;
    private final String culturalSignificance;
    private final String narrativeAngle;
    private final String storySummary;
    /** GPT-generated character sheet — fiziksel tanımlar, kıyafetler, yaş, mekan/dönem. Her türküye özel. */
    private final String characterSheet;
    private final TurkuSourceConfidence sourceConfidence;
    private final boolean needsHumanReview;
    private final boolean copyrightReviewRequired;
    private final List<String> reviewFlags;

    // ── 3-Stage Pipeline Outputs ──
    /** 1. aşama: Kaynaklı araştırma raporu */
    private final String researchReport;
    /** 2. aşama: Hikâye taslağı */
    private final String storyDraft;
    /** 3. aşama: Editörden geçmiş nihai hikâye */
    private final String finalStory;
    /** 3. aşama: Editör kalite kontrol kaydı */
    private final String qualityStatus;

    private TurkuStoryPackage(Builder b) {
        this.turkuName = b.turkuName;
        this.regionContext = b.regionContext;
        this.originSummary = b.originSummary;
        this.emotionalTheme = b.emotionalTheme;
        this.culturalSignificance = b.culturalSignificance;
        this.narrativeAngle = b.narrativeAngle;
        this.storySummary = b.storySummary;
        this.characterSheet = b.characterSheet;
        this.sourceConfidence = b.sourceConfidence;
        this.needsHumanReview = b.needsHumanReview;
        this.copyrightReviewRequired = b.copyrightReviewRequired;
        this.reviewFlags = b.reviewFlags != null ? List.copyOf(b.reviewFlags) : List.of();
        this.researchReport = b.researchReport;
        this.storyDraft = b.storyDraft;
        this.finalStory = b.finalStory;
        this.qualityStatus = b.qualityStatus;
    }

    public String getTurkuName() { return turkuName; }
    public String getRegionContext() { return regionContext; }
    public String getOriginSummary() { return originSummary; }
    public String getEmotionalTheme() { return emotionalTheme; }
    public String getCulturalSignificance() { return culturalSignificance; }
    public String getNarrativeAngle() { return narrativeAngle; }
    public String getStorySummary() { return storySummary; }
    public String getCharacterSheet() { return characterSheet; }
    public TurkuSourceConfidence getSourceConfidence() { return sourceConfidence; }
    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public List<String> getReviewFlags() { return reviewFlags; }
    public String getResearchReport() { return researchReport; }
    public String getStoryDraft() { return storyDraft; }
    public String getFinalStory() { return finalStory; }
    public String getQualityStatus() { return qualityStatus; }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String turkuName;
        private String regionContext;
        private String originSummary;
        private String emotionalTheme;
        private String culturalSignificance;
        private String narrativeAngle;
        private String storySummary;
        private String characterSheet;
        private TurkuSourceConfidence sourceConfidence = TurkuSourceConfidence.UNKNOWN;
        private boolean needsHumanReview = true;
        private boolean copyrightReviewRequired = true;
        private List<String> reviewFlags;
        private String researchReport;
        private String storyDraft;
        private String finalStory;
        private String qualityStatus;

        public Builder turkuName(String v) { this.turkuName = v; return this; }
        public Builder regionContext(String v) { this.regionContext = v; return this; }
        public Builder originSummary(String v) { this.originSummary = v; return this; }
        public Builder emotionalTheme(String v) { this.emotionalTheme = v; return this; }
        public Builder culturalSignificance(String v) { this.culturalSignificance = v; return this; }
        public Builder narrativeAngle(String v) { this.narrativeAngle = v; return this; }
        public Builder storySummary(String v) { this.storySummary = v; return this; }
        public Builder characterSheet(String v) { this.characterSheet = v; return this; }
        public Builder sourceConfidence(TurkuSourceConfidence v) { this.sourceConfidence = v; return this; }
        public Builder needsHumanReview(boolean v) { this.needsHumanReview = v; return this; }
        public Builder copyrightReviewRequired(boolean v) { this.copyrightReviewRequired = v; return this; }
        public Builder reviewFlags(List<String> v) { this.reviewFlags = v; return this; }
        public Builder researchReport(String v) { this.researchReport = v; return this; }
        public Builder storyDraft(String v) { this.storyDraft = v; return this; }
        public Builder finalStory(String v) { this.finalStory = v; return this; }
        public Builder qualityStatus(String v) { this.qualityStatus = v; return this; }

        public TurkuStoryPackage build() { return new TurkuStoryPackage(this); }
    }
}
