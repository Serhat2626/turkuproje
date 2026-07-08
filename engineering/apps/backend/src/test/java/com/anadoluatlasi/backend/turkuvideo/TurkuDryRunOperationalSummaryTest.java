package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FP-053 — Dry Run Operational Summary")
class TurkuDryRunOperationalSummaryTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl", "storageProvider",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia");

    private TurkuInternalDryRunRecordService recordService;
    private TurkuInternalDryRunOperationalSummaryService summaryService;
    private TurkuStoryboardInternalRenderChainHarness harness;

    @BeforeEach void setUp() {
        recordService = new TurkuInternalDryRunRecordService();
        summaryService = new TurkuInternalDryRunOperationalSummaryService();
        harness = new TurkuStoryboardInternalRenderChainHarness();
    }

    private void addValid(int scenes) {
        var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(scenes);
        recordService.createFromDryRunResponse(new TurkuInternalDryRunTriggerResponse(harness.execute(fixture)));
    }
    private void addRejected() { recordService.createFromDryRunResponse(null); }
    private void addFailed() {
        recordService.createRejectedRecord("fail",
                List.of(TurkuInternalDryRunRecordIssueCode.DRY_RUN_CHAIN_FAILED));
    }

    // ── A. Valid Records Summary ──
    @Nested @DisplayName("A. Valid Records Summary")
    class ValidSummary {
        @Test @DisplayName("single valid → ALL_ACCEPTED")
        void single() {
            addValid(3);
            var s = summaryService.summarize(recordService);
            assertThat(s.getTotalRecordCount()).isEqualTo(1);
            assertThat(s.getAcceptedCount()).isEqualTo(1);
            assertThat(s.getOverallStatus()).isEqualTo(TurkuInternalDryRunOperationalSummaryStatus.ALL_ACCEPTED);
            assertThat(s.getTotalSceneCount()).isEqualTo(3);
            assertThat(s.getTotalClipCount()).isEqualTo(3);
            assertThat(s.getStitchedCount()).isEqualTo(1);
            assertThat(s.isInternalOnly()).isTrue();
        }
        @Test @DisplayName("multiple valid → ALL_ACCEPTED with aggregate counts")
        void multiple() {
            addValid(3); addValid(2); addValid(4);
            var s = summaryService.summarize(recordService);
            assertThat(s.getTotalRecordCount()).isEqualTo(3);
            assertThat(s.getAcceptedCount()).isEqualTo(3);
            assertThat(s.getTotalSceneCount()).isEqualTo(9);
            assertThat(s.getOverallStatus()).isEqualTo(TurkuInternalDryRunOperationalSummaryStatus.ALL_ACCEPTED);
        }
    }

    // ── B. Failed/Rejected Summary ──
    @Nested @DisplayName("B. Failed/Rejected Summary")
    class FailedRejected {
        @Test @DisplayName("all rejected → ALL_REJECTED")
        void allRejected() {
            addRejected(); addRejected();
            var s = summaryService.summarize(recordService);
            assertThat(s.getOverallStatus()).isEqualTo(TurkuInternalDryRunOperationalSummaryStatus.ALL_REJECTED);
            assertThat(s.getRejectedCount()).isEqualTo(2);
            assertThat(s.getIssueCodes()).contains(TurkuInternalDryRunOperationalSummaryIssueCode.HIGH_REJECTION_RATE);
        }
        @Test @DisplayName("mixed → MIXED")
        void mixed() {
            addValid(2); addRejected(); addFailed();
            var s = summaryService.summarize(recordService);
            assertThat(s.getOverallStatus()).isEqualTo(TurkuInternalDryRunOperationalSummaryStatus.MIXED);
        }
    }

    // ── C. Empty History ──
    @Nested @DisplayName("C. Empty History")
    class EmptyHistory {
        @Test @DisplayName("no records → NO_RECORDS with issue code")
        void empty() {
            var s = summaryService.summarize(recordService);
            assertThat(s.getTotalRecordCount()).isEqualTo(0);
            assertThat(s.getOverallStatus()).isEqualTo(TurkuInternalDryRunOperationalSummaryStatus.NO_RECORDS);
            assertThat(s.getIssueCodes()).contains(TurkuInternalDryRunOperationalSummaryIssueCode.NO_RECORDS_AVAILABLE);
            assertThat(s.isInternalOnly()).isTrue();
        }
    }

    // ── D. Forbidden Field Sweep ──
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("TurkuInternalDryRunOperationalSummary — no forbidden")
        void summary() { check(TurkuInternalDryRunOperationalSummary.class); }
        @Test @DisplayName("TurkuInternalDryRunOperationalSummaryService — no forbidden")
        void service() { check(TurkuInternalDryRunOperationalSummaryService.class); }
        private void check(Class<?> c) {
            Set<String> fields = Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN)
                assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
        }
    }

    // ── E. No Publish/Export Statuses ──
    @Nested @DisplayName("E. No Publish/Export Statuses")
    class NoPublish {
        @Test @DisplayName("summary status — no forbidden")
        void status() {
            for (var v : TurkuInternalDryRunOperationalSummaryStatus.values()) {
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH");
                assertThat(v.name()).doesNotContain("COMPLETED");
                assertThat(v.name()).doesNotContain("AUTO_APPROVED");
            }
        }
        @Test @DisplayName("exactly 5 summary statuses")
        void count() { assertThat(TurkuInternalDryRunOperationalSummaryStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 5 summary issue codes")
        void issueCount() { assertThat(TurkuInternalDryRunOperationalSummaryIssueCode.values()).hasSize(5); }
    }

    // ── F. Endpoint Inventory Unchanged ──
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total: 46 = 26 POST + 20 GET")
        void total() {
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

    // ── G. ProcessBuilder Boundary ──
    @Nested @DisplayName("G. ProcessBuilder Boundary")
    class ProcessBoundary {
        @Test @DisplayName("FP-053 classes no ProcessBuilder/Runtime")
        void clean() {
            for (var c : List.of(TurkuInternalDryRunOperationalSummary.class,
                    TurkuInternalDryRunOperationalSummaryService.class)) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
