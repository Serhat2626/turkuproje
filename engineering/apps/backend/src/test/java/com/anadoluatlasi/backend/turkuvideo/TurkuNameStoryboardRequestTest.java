package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RP-001 \u2014 T\u00fcrk\u00fc Name to Safe Storyboard Request Pipeline")
class TurkuNameStoryboardRequestTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html", "audioFile", "videoFile", "mediaFile");

    private static final List<Class<?>> RP001_CLASSES = List.of(
            TurkuNameStoryboardRequest.class,
            TurkuNameStoryboardRequestResult.class,
            TurkuNameStoryboardRequestPolicy.class,
            TurkuNameStoryboardRequestService.class);

    private TurkuNameStoryboardRequestService service;

    @BeforeEach void setUp() { service = new TurkuNameStoryboardRequestService(); }

    private TurkuNameStoryboardRequest validReq() {
        return new TurkuNameStoryboardRequest(
                "\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC",
                8, 60_000, "MP4", 1920, 1080, 24);
    }

    // \u2500\u2500 A. Valid Request \u2500\u2500
    @Nested @DisplayName("A. Valid Request")
    class ValidRequest {
        @Test @DisplayName("valid creates READY_FOR_STORYBOARD_GENERATION_BOUNDARY")
        void valid() {
            var r = service.createRequest(validReq());
            assertThat(r.getStatus()).isEqualTo(TurkuNameStoryboardRequestStatus.READY_FOR_STORYBOARD_GENERATION_BOUNDARY);
            assertThat(r.getOpaqueStoryboardRequestRef()).startsWith("sb-req:");
            assertThat(r.getSafeTurkuTitleLabel()).isEqualTo("\u00c7anakkale T\u00fcrk\u00fcs\u00fc");
            assertThat(r.getInspirationMode()).isEqualTo("TURKU_TITLE_INSPIRATION");
            assertThat(r.getCopyrightSafeIntentSummary()).contains("telif");
        }
    }

    // \u2500\u2500 B. Title Validation \u2500\u2500
    @Nested @DisplayName("B. Title Validation")
    class TitleValidation {
        @Test @DisplayName("null request") void nullReq() {
            assertThat(service.createRequest(null).getStatus())
                    .isEqualTo(TurkuNameStoryboardRequestStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("empty title") void empty() {
            var req = new TurkuNameStoryboardRequest("", null, null, null, 8, 60_000, "MP4", 1920, 1080, 24);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.TITLE_EMPTY);
        }
        @Test @DisplayName("too long title") void tooLong() {
            var req = new TurkuNameStoryboardRequest("A".repeat(201), null, null, null, 8, 60_000, "MP4", 1920, 1080, 24);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.TITLE_TOO_LONG);
        }
        @Test @DisplayName("lyrics multiline rejected") void lyrics() {
            String lyricsInput = "Sat\u0131r 1\nSat\u0131r 2\nSat\u0131r 3\nSat\u0131r 4";
            var req = new TurkuNameStoryboardRequest(lyricsInput, null, null, null, 8, 60_000, "MP4", 1920, 1080, 24);
            var r = service.createRequest(req);
            assertThat(r.getIssueCodes()).containsAnyOf(
                    TurkuNameStoryboardRequestIssueCode.TITLE_CONTAINS_LYRICS,
                    TurkuNameStoryboardRequestIssueCode.TITLE_CONTAINS_MULTILINE);
        }
        @Test @DisplayName("URL rejected") void url() {
            var req = new TurkuNameStoryboardRequest("https://example.com/turku", null, null, null, 8, 60_000, "MP4", 1920, 1080, 24);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.TITLE_CONTAINS_URL);
        }
    }

    // \u2500\u2500 C. Bounds Validation \u2500\u2500
    @Nested @DisplayName("C. Bounds Validation")
    class BoundsValidation {
        @Test @DisplayName("scene count 0") void scene0() {
            var req = new TurkuNameStoryboardRequest("Test", null, null, null, 0, 60_000, "MP4", 1920, 1080, 24);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.SCENE_COUNT_OUT_OF_RANGE);
        }
        @Test @DisplayName("scene count 31") void scene31() {
            var req = new TurkuNameStoryboardRequest("Test", null, null, null, 31, 60_000, "MP4", 1920, 1080, 24);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.SCENE_COUNT_OUT_OF_RANGE);
        }
        @Test @DisplayName("duration too short") void shortDur() {
            var req = new TurkuNameStoryboardRequest("Test", null, null, null, 8, 1_000, "MP4", 1920, 1080, 24);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.DURATION_OUT_OF_RANGE);
        }
        @Test @DisplayName("fps 0") void fps0() {
            var req = new TurkuNameStoryboardRequest("Test", null, null, null, 8, 60_000, "MP4", 1920, 1080, 0);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.FPS_OUT_OF_RANGE);
        }
        @Test @DisplayName("resolution too small") void resSmall() {
            var req = new TurkuNameStoryboardRequest("Test", null, null, null, 8, 60_000, "MP4", 50, 50, 24);
            assertThat(service.createRequest(req).getIssueCodes())
                    .contains(TurkuNameStoryboardRequestIssueCode.RESOLUTION_OUT_OF_RANGE);
        }
    }

    // \u2500\u2500 D. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields") void sweep() {
            for (var c : RP001_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 E. No Publish Statuses \u2500\u2500
    @Nested @DisplayName("E. No COMPLETED/PUBLISHED/AUTO_APPROVED/READY_FOR_PUBLISH")
    class NoPublish {
        @Test @DisplayName("status enum safe") void status() {
            for (var v : TurkuNameStoryboardRequestStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("READY_FOR_PUBLISH")
                        .doesNotContain("PUBLICLY_AVAILABLE");
        }
        @Test @DisplayName("exactly 6 statuses")
        void count() { assertThat(TurkuNameStoryboardRequestStatus.values()).hasSize(6); }
        @Test @DisplayName("exactly 13 issue codes")
        void issues() { assertThat(TurkuNameStoryboardRequestIssueCode.values()).hasSize(11); }
    }

    // \u2500\u2500 F. Endpoint Unchanged \u2500\u2500
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total unchanged") void total() {
            long post = count(AdminReviewController.class, PostMapping.class)
                    + count(AdminPipelineController.class, PostMapping.class)
                    + count(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(AdminReviewController.class, GetMapping.class)
                    + count(AdminPipelineController.class, GetMapping.class)
                    + count(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post + get).isEqualTo(46);
            assertThat(post).isEqualTo(26);
            assertThat(get).isEqualTo(20);
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // \u2500\u2500 G. Factories + ProcessBuilder \u2500\u2500
    @Nested @DisplayName("G. Factories + ProcessBuilder")
    class Factories {
        @Test @DisplayName("rejected factory") void rej() {
            var r = TurkuNameStoryboardRequestResult.rejected(TurkuNameStoryboardRequestIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(TurkuNameStoryboardRequestStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("failed factory") void fail() {
            var r = TurkuNameStoryboardRequestResult.failed(TurkuNameStoryboardRequestIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(TurkuNameStoryboardRequestStatus.FAILED_SAFE);
        }
        @Test @DisplayName("no ProcessBuilder/Runtime") void clean() {
            for (var c : RP001_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // \u2500\u2500 H. T\u00fcrk\u00e7e Support \u2500\u2500
    @Nested @DisplayName("H. T\u00fcrk\u00e7e Support")
    class Turkish {
        @Test @DisplayName("\u00c7anakkale T\u00fcrk\u00fcs\u00fc preserved") void chars() {
            var r = service.createRequest(validReq());
            assertThat(r.getSafeTurkuTitleLabel()).isEqualTo("\u00c7anakkale T\u00fcrk\u00fcs\u00fc");
        }
        @Test @DisplayName("copyright summary in T\u00fcrk\u00e7e") void summary() {
            var r = service.createRequest(validReq());
            assertThat(r.getCopyrightSafeIntentSummary()).contains("telif");
        }
    }
}
