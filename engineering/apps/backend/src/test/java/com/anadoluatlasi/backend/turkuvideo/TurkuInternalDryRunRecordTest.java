package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-051 — Admin Internal Dry Run Record & Audit-Safe History Foundation.
 */
@DisplayName("FP-051 — Dry Run Record & History")
class TurkuInternalDryRunRecordTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl", "storageProvider",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia");

    private TurkuInternalDryRunRecordService recordService;
    private TurkuStoryboardInternalRenderChainHarness harness;

    @BeforeEach
    void setUp() {
        recordService = new TurkuInternalDryRunRecordService();
        harness = new TurkuStoryboardInternalRenderChainHarness();
    }

    // ── A. Valid Dry Run → Safe Record ──
    @Nested @DisplayName("A. Valid Dry Run → Safe Record")
    class ValidRecord {
        @Test @DisplayName("valid trigger response creates READY_FOR_ADMIN_HISTORY_ONLY record")
        void validRecord() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(fixture));
            var record = recordService.createFromDryRunResponse(response);
            assertThat(record.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.READY_FOR_ADMIN_HISTORY_ONLY);
            assertThat(record.getSceneCount()).isEqualTo(3);
            assertThat(record.getGeneratedClipCount()).isEqualTo(3);
            assertThat(record.isStitchedArtifactPresent()).isTrue();
            assertThat(record.getOpaqueRecordRef()).startsWith("dryrec:");
        }
        @Test @DisplayName("record has opaque refs only")
        void opaqueRefs() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(fixture));
            var record = recordService.createFromDryRunResponse(response);
            assertThat(record.getOpaqueRecordRef()).isNotNull();
            assertThat(record.getOpaqueDryRunRef()).isNotNull();
            assertThat(record.getOpaqueStoryboardRef()).isNotNull();
            assertThat(record.getOpaqueReviewPackageRef()).isNotNull();
        }
    }

    // ── B. Rejected/Failed Record ──
    @Nested @DisplayName("B. Rejected/Failed Record")
    class RejectedRecord {
        @Test @DisplayName("null response creates REJECTED_UNSAFE record")
        void nullResponse() {
            var record = recordService.createFromDryRunResponse(null);
            assertThat(record.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.REJECTED_UNSAFE);
            assertThat(record.getIssueCodes()).contains(TurkuInternalDryRunRecordIssueCode.TRIGGER_REJECTED);
        }
        @Test @DisplayName("rejected response creates rejected record")
        void rejectedResponse() {
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(null));
            var record = recordService.createFromDryRunResponse(response);
            assertThat(record.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("createRejectedRecord directly")
        void directRejected() {
            var record = recordService.createRejectedRecord("test:ref",
                    List.of(TurkuInternalDryRunRecordIssueCode.INVALID_FIXTURE_INPUT));
            assertThat(record.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.REJECTED_UNSAFE);
            assertThat(record.getIssueCodes()).contains(TurkuInternalDryRunRecordIssueCode.INVALID_FIXTURE_INPUT);
        }
    }

    // ── C. Record Summary ──
    @Nested @DisplayName("C. Record Summary Safe Metadata")
    class RecordSummary {
        @Test @DisplayName("summary exposes safe metadata")
        void safeSummary() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(fixture));
            var record = recordService.createFromDryRunResponse(response);
            var summary = recordService.getSummary(record);
            assertThat(summary.isInternalOnly()).isTrue();
            assertThat(summary.getSceneCount()).isEqualTo(3);
            assertThat(summary.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.READY_FOR_ADMIN_HISTORY_ONLY);
            assertThat(summary.getOpaqueRecordRef()).startsWith("dryrec:");
        }
        @Test @DisplayName("summary internalOnly always true")
        void alwaysInternal() {
            var record = recordService.createRejectedRecord("ref", List.of());
            assertThat(recordService.getSummary(record).isInternalOnly()).isTrue();
        }
    }

    // ── D. History ──
    @Nested @DisplayName("D. History")
    class History {
        @Test @DisplayName("history accumulates records")
        void historyAccumulates() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            recordService.createFromDryRunResponse(new TurkuInternalDryRunTriggerResponse(harness.execute(fixture)));
            recordService.createFromDryRunResponse(null);
            recordService.createRejectedRecord("test", List.of());
            assertThat(recordService.getRecordCount()).isEqualTo(3);
            assertThat(recordService.getHistory()).hasSize(3);
        }
        @Test @DisplayName("all history entries are internalOnly")
        void allInternal() {
            recordService.createFromDryRunResponse(new TurkuInternalDryRunTriggerResponse(
                    harness.execute(TurkuStoryboardInternalRenderChainFixture.turkuFixture(2))));
            recordService.createRejectedRecord("x", List.of());
            for (var s : recordService.getHistory())
                assertThat(s.isInternalOnly()).isTrue();
        }
    }

    // ── E. Forbidden Field Sweep ──
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("TurkuInternalDryRunRecord — no forbidden fields")
        void record() { checkFields(TurkuInternalDryRunRecord.class); }
        @Test @DisplayName("TurkuInternalDryRunRecordSummary — no forbidden fields")
        void summary() { checkFields(TurkuInternalDryRunRecordSummary.class); }
        @Test @DisplayName("TurkuInternalDryRunRecordService — no forbidden fields")
        void service() { checkFields(TurkuInternalDryRunRecordService.class); }

        private void checkFields(Class<?> c) {
            Set<String> fields = Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN)
                assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
        }
    }

    // ── F. No Publish/Export Statuses ──
    @Nested @DisplayName("F. No Publish/Export Statuses")
    class NoPublish {
        @Test @DisplayName("record status has no forbidden values")
        void status() {
            for (var v : TurkuInternalDryRunRecordStatus.values()) {
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH");
                assertThat(v.name()).doesNotContain("COMPLETED");
                assertThat(v.name()).doesNotContain("AUTO_APPROVED");
                assertThat(v.name()).doesNotContain("EXPORTED");
            }
        }
        @Test @DisplayName("record status has exactly 7 values")
        void count() {
            assertThat(TurkuInternalDryRunRecordStatus.values()).hasSize(7);
        }
        @Test @DisplayName("record issue code has exactly 10 values")
        void issueCount() {
            assertThat(TurkuInternalDryRunRecordIssueCode.values()).hasSize(10);
        }
    }

    // ── G. Endpoint Inventory Unchanged ──
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class EndpointInventory {
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

    // ── H. ProcessBuilder Boundary ──
    @Nested @DisplayName("H. ProcessBuilder Boundary")
    class ProcessBuilderBoundary {
        @Test @DisplayName("FP-051 classes have no ProcessBuilder/Runtime fields")
        void clean() {
            for (var c : List.of(TurkuInternalDryRunRecord.class, TurkuInternalDryRunRecordSummary.class,
                    TurkuInternalDryRunRecordService.class)) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName())
                        .doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
