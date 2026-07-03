package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-004 Task 09: OpenAiPlatformPackageResponseParser testleri.
 */
@DisplayName("FP-004 Task 09: OpenAiPlatformPackageResponseParser")
class OpenAiPlatformPackageResponseParserTest {

    private static final String VALID_4PKG_JSON = """
            {"packages":[
              {"packageType":"YOUTUBE_LONG_FORM","title":"YT Ba\u015fl\u0131k","captionOrDescription":"YT A\u00e7\u0131klama",
               "hashtags":["#tarih"],"callToAction":"Abone ol","targetDurationSeconds":600,"aspectRatio":"16:9",
               "videoTitle":"SEO Ba\u015fl\u0131k","longDescription":"Detayl\u0131 a\u00e7\u0131klama",
               "chapterOutline":["00:00 Giri\u015f"],"pinnedCommentSuggestion":"Yorum",
               "thumbnailTextSuggestion":"Thumbnail","tags":["tarih"],
               "sceneUsages":[{"sceneNumber":1,"usageType":"PRIMARY","included":true,"adaptationNote":"Tam"}]},
              {"packageType":"YOUTUBE_SHORTS","title":"Shorts","captionOrDescription":"Shorts desc",
               "hashtags":["#shorts"],"callToAction":"Like","targetDurationSeconds":60,"aspectRatio":"9:16",
               "shortHook":"Hook!","shortCaption":"Caption","overlayTextSuggestions":["Overlay"],
               "shortHashtagSet":["#shorts"],"shortCta":"Like","durationGuidance":"30-60sn"},
              {"packageType":"INSTAGRAM_REELS","title":"Reels","captionOrDescription":"Reels desc",
               "hashtags":["#reels"],"callToAction":"Takip","targetDurationSeconds":30,"aspectRatio":"9:16",
               "shortHook":"IG Hook","shortCaption":"IG Cap","overlayTextSuggestions":["IG OL"],
               "shortHashtagSet":["#ig"],"shortCta":"Takip","durationGuidance":"15-30sn"},
              {"packageType":"FACEBOOK_REELS","title":"FB Reels","captionOrDescription":"FB desc",
               "hashtags":["#fb"],"callToAction":"Payla\u015f","targetDurationSeconds":45,"aspectRatio":"9:16",
               "shortHook":"FB Hook","shortCaption":"FB Cap","overlayTextSuggestions":["FB OL"],
               "shortHashtagSet":["#fb"],"shortCta":"Payla\u015f","durationGuidance":"30-60sn"}
            ]}""";

    // ── Successful Parsing ──────────────────────────────────────────

    @Nested
    @DisplayName("SuccessfulParsing")
    class SuccessfulParsing {

        @Test
        @DisplayName("4 platform paketi ile ge\u00e7erli JSON parse edilir")
        void parsesValid4PackageJson() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    VALID_4PKG_JSON, "OPENAI", "gpt-4o-mini");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPackages()).hasSize(4);
            assertThat(result.getProviderType()).isEqualTo("OPENAI");
            assertThat(result.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("Markdown code fence JSON parse edilir")
        void parsesCodeFenceJson() {
            String fenced = "```json\n" + VALID_4PKG_JSON + "\n```";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    fenced, "OPENAI", "gpt-4o-mini");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPackages()).hasSize(4);
        }

        @Test
        @DisplayName("Bilinmeyen ekstra alanlar g\u00fcvenle ignore edilir")
        void ignoresUnknownExtraFields() {
            String jsonWithExtra = VALID_4PKG_JSON.replace(
                    "\"packageType\":\"YOUTUBE_LONG_FORM\"",
                    "\"unknownField\":\"value\",\"packageType\":\"YOUTUBE_LONG_FORM\"");
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    jsonWithExtra, "OPENAI", "gpt-4o-mini");

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Scene usages normalize edilir")
        void normalizesSceneUsages() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    VALID_4PKG_JSON, "OPENAI", "gpt-4o-mini");

            assertThat(result.getPackages().get(0).getSceneUsages()).isNotNull();
            assertThat(result.getPackages().get(0).getSceneUsages().get(0).getSceneNumber()).isEqualTo(1);
            assertThat(result.getPackages().get(0).getSceneUsages().get(0).getUsageType()).isEqualTo("PRIMARY");
            assertThat(result.getPackages().get(0).getSceneUsages().get(0).isIncluded()).isTrue();
        }
    }

    // ── Empty / Invalid Responses ────────────────────────────────────

    @Nested
    @DisplayName("EmptyInvalidResponses")
    class EmptyInvalidResponses {

        @Test
        @DisplayName("Bo\u015f yan\u0131t \u2192 EMPTY_AI_RESPONSE")
        void emptyResponse() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    "", "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
        }

        @Test
        @DisplayName("Null yan\u0131t \u2192 EMPTY_AI_RESPONSE")
        void nullResponse() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    null, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
        }

        @Test
        @DisplayName("Ge\u00e7ersiz JSON \u2192 INVALID_AI_RESPONSE_FORMAT")
        void invalidJson() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    "not json at all", "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Packages eksik \u2192 INVALID_AI_RESPONSE_FORMAT")
        void missingPackages() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    "{}", "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Bo\u015f packages listesi \u2192 INVALID_AI_RESPONSE_FORMAT")
        void emptyPackagesList() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    "{\"packages\":[]}", "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }
    }

    // ── Package Type Validation ──────────────────────────────────────

    @Nested
    @DisplayName("PackageTypeValidation")
    class PackageTypeValidation {

        @Test
        @DisplayName("Tekrarlanan paket tipi \u2192 failure")
        void duplicatePackageType() {
            String dupe = """
                    {"packages":[
                      {"packageType":"YOUTUBE_LONG_FORM","title":"T1","captionOrDescription":"D1","callToAction":"C1",
                       "videoTitle":"VT","longDescription":"LD","chapterOutline":["ch"],"pinnedCommentSuggestion":"pc","thumbnailTextSuggestion":"tt"},
                      {"packageType":"YOUTUBE_LONG_FORM","title":"T2","captionOrDescription":"D2","callToAction":"C2",
                       "videoTitle":"VT2","longDescription":"LD2","chapterOutline":["ch2"],"pinnedCommentSuggestion":"pc2","thumbnailTextSuggestion":"tt2"}
                    ]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    dupe, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
            assertThat(result.getErrorMessage()).contains("tekrarlanan");
        }

        @Test
        @DisplayName("Bilinmeyen paket tipi \u2192 failure")
        void unknownPackageType() {
            String unknown = """
                    {"packages":[{"packageType":"TIKTOK","title":"T","captionOrDescription":"D","callToAction":"C"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    unknown, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
            assertThat(result.getErrorMessage()).contains("bilinmeyen");
        }
    }

    // ── Common Field Validation ──────────────────────────────────────

    @Nested
    @DisplayName("CommonFieldValidation")
    class CommonFieldValidation {

        @Test
        @DisplayName("title eksik \u2192 failure")
        void missingTitle() {
            String noTitle = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noTitle, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("title");
        }

        @Test
        @DisplayName("captionOrDescription eksik \u2192 failure")
        void missingCaption() {
            String noCaption = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","title":"T","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noCaption, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("captionOrDescription");
        }

        @Test
        @DisplayName("callToAction eksik \u2192 failure")
        void missingCta() {
            String noCta = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","title":"T","captionOrDescription":"D",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noCta, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("callToAction");
        }
    }

    // ── YouTube Long-Form Validation ─────────────────────────────────

    @Nested
    @DisplayName("YouTubeLongFormValidation")
    class YouTubeLongFormValidation {

        @Test
        @DisplayName("videoTitle eksik \u2192 failure")
        void missingVideoTitle() {
            String noVT = """
                    {"packages":[{"packageType":"YOUTUBE_LONG_FORM","title":"T","captionOrDescription":"D","callToAction":"C",
                     "longDescription":"LD","chapterOutline":["ch"],"pinnedCommentSuggestion":"pc","thumbnailTextSuggestion":"tt"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noVT, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("videoTitle");
        }

        @Test
        @DisplayName("longDescription eksik \u2192 failure")
        void missingLongDescription() {
            String noLD = """
                    {"packages":[{"packageType":"YOUTUBE_LONG_FORM","title":"T","captionOrDescription":"D","callToAction":"C",
                     "videoTitle":"VT","chapterOutline":["ch"],"pinnedCommentSuggestion":"pc","thumbnailTextSuggestion":"tt"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noLD, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("longDescription");
        }

        @Test
        @DisplayName("chapterOutline eksik \u2192 failure")
        void missingChapterOutline() {
            String noCH = """
                    {"packages":[{"packageType":"YOUTUBE_LONG_FORM","title":"T","captionOrDescription":"D","callToAction":"C",
                     "videoTitle":"VT","longDescription":"LD","pinnedCommentSuggestion":"pc","thumbnailTextSuggestion":"tt"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noCH, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("chapterOutline");
        }

        @Test
        @DisplayName("pinnedCommentSuggestion eksik \u2192 failure")
        void missingPinnedComment() {
            String noPC = """
                    {"packages":[{"packageType":"YOUTUBE_LONG_FORM","title":"T","captionOrDescription":"D","callToAction":"C",
                     "videoTitle":"VT","longDescription":"LD","chapterOutline":["ch"],"thumbnailTextSuggestion":"tt"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noPC, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("pinnedCommentSuggestion");
        }

        @Test
        @DisplayName("thumbnailTextSuggestion eksik \u2192 failure")
        void missingThumbnailText() {
            String noTT = """
                    {"packages":[{"packageType":"YOUTUBE_LONG_FORM","title":"T","captionOrDescription":"D","callToAction":"C",
                     "videoTitle":"VT","longDescription":"LD","chapterOutline":["ch"],"pinnedCommentSuggestion":"pc"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noTT, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("thumbnailTextSuggestion");
        }
    }

    // ── Short-Form Validation ────────────────────────────────────────

    @Nested
    @DisplayName("ShortFormValidation")
    class ShortFormValidation {

        @Test
        @DisplayName("shortHook eksik \u2192 failure")
        void missingShortHook() {
            String noHook = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noHook, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("shortHook");
        }

        @Test
        @DisplayName("shortCaption eksik \u2192 failure")
        void missingShortCaption() {
            String noCap = """
                    {"packages":[{"packageType":"INSTAGRAM_REELS","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noCap, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("shortCaption");
        }

        @Test
        @DisplayName("overlayTextSuggestions eksik \u2192 failure")
        void missingOverlay() {
            String noOL = """
                    {"packages":[{"packageType":"FACEBOOK_REELS","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noOL, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("overlayTextSuggestions");
        }

        @Test
        @DisplayName("shortHashtagSet eksik \u2192 failure")
        void missingHashtags() {
            String noHT = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noHT, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("shortHashtagSet");
        }

        @Test
        @DisplayName("shortCta eksik \u2192 failure")
        void missingShortCta() {
            String noCTA = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noCTA, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("shortCta");
        }

        @Test
        @DisplayName("durationGuidance eksik \u2192 failure")
        void missingDurationGuidance() {
            String noDG = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    noDG, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("durationGuidance");
        }
    }

    // ── Safety / Governance ──────────────────────────────────────────

    @Nested
    @DisplayName("SafetyGovernance")
    class SafetyGovernance {

        @Test
        @DisplayName("AI-supplied APPROVED status \u2192 failure")
        void rejectsApprovedStatus() {
            String withApproved = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","status":"APPROVED","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    withApproved, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
        }

        @Test
        @DisplayName("AI-supplied READY_FOR_PUBLISH status \u2192 failure")
        void rejectsReadyForPublishStatus() {
            String withRFP = """
                    {"packages":[{"packageType":"YOUTUBE_SHORTS","status":"READY_FOR_PUBLISH","title":"T","captionOrDescription":"D","callToAction":"C",
                     "shortHook":"H","shortCaption":"SC","overlayTextSuggestions":["O"],"shortHashtagSet":["#h"],"shortCta":"CT","durationGuidance":"DG"}]}""";
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    withRFP, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
        }

        @Test
        @DisplayName("Refusal i\u00e7erik \u2192 AI_RESPONSE_UNSAFE_CONTENT")
        void refusalContent() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    "I cannot generate this content due to safety guidelines", "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
        }

        @Test
        @DisplayName("T\u00fcrk\u00e7e refusal i\u00e7erik \u2192 AI_RESPONSE_UNSAFE_CONTENT")
        void turkishRefusalContent() {
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    "Bu iste\u011fi yerine getiremem", "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
        }

        @Test
        @DisplayName("AI_GENERATED status kabul edilir (filtered out, not rejected)")
        void acceptsAiGeneratedStatus() {
            String withAiGen = VALID_4PKG_JSON.replace(
                    "\"packageType\":\"YOUTUBE_LONG_FORM\"",
                    "\"status\":\"AI_GENERATED\",\"packageType\":\"YOUTUBE_LONG_FORM\"");
            PlatformPackageAgentResponse result = OpenAiPlatformPackageResponseParser.parse(
                    withAiGen, "OPENAI", "gpt-4o-mini");
            assertThat(result.isSuccess()).isTrue();
        }
    }
}
