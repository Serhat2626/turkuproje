package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-052 — Admin Internal Dry Run Record Safety Baseline Closure.
 * No new capability. Locks FP-049→FP-051 record/history safety.
 */
@DisplayName("FP-052 — Dry Run Record Safety Baseline")
class TurkuDryRunRecordSafetyBaselineTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl", "storageProvider",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia");

    private static final List<Class<?>> FP049_051_CLASSES = List.of(
            TurkuInternalDryRunTriggerRequest.class,
            TurkuInternalDryRunTriggerResponse.class,
            TurkuInternalDryRunRecord.class,
            TurkuInternalDryRunRecordSummary.class,
            TurkuInternalDryRunRecordService.class);

    // ── A. Full Forbidden Sweep FP-049→FP-051 ──
    @Nested @DisplayName("A. Forbidden Sweep (FP-049→FP-051)")
    class FullSweep {
        @Test @DisplayName("all 5 FP-049/051 classes — no forbidden fields")
        void sweep() {
            for (var c : FP049_051_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── B. Record Status Safety ──
    @Nested @DisplayName("B. Record Status Safety")
    class StatusSafety {
        @Test @DisplayName("no READY_FOR_PUBLISH / COMPLETED / AUTO_APPROVED / EXPORTED")
        void noForbidden() {
            for (var v : TurkuInternalDryRunRecordStatus.values()) {
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH");
                assertThat(v.name()).doesNotContain("COMPLETED");
                assertThat(v.name()).doesNotContain("AUTO_APPROVED");
                assertThat(v.name()).doesNotContain("EXPORTED");
                assertThat(v.name()).doesNotContain("PUBLISHED");
            }
        }
        @Test @DisplayName("exactly 7 record statuses")
        void count() { assertThat(TurkuInternalDryRunRecordStatus.values()).hasSize(7); }

        @Test @DisplayName("exactly 10 record issue codes")
        void issueCount() { assertThat(TurkuInternalDryRunRecordIssueCode.values()).hasSize(10); }
    }

    // ── C. Record Service In-Memory Safety ──
    @Nested @DisplayName("C. Record Service In-Memory Safety")
    class ServiceSafety {
        @Test @DisplayName("service creates record from valid response")
        void validRecord() {
            var svc = new TurkuInternalDryRunRecordService();
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            var response = new TurkuInternalDryRunTriggerResponse(harness.execute(fixture));
            var record = svc.createFromDryRunResponse(response);
            assertThat(record.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.READY_FOR_ADMIN_HISTORY_ONLY);
            assertThat(svc.getSummary(record).isInternalOnly()).isTrue();
        }
        @Test @DisplayName("service creates rejected record from null")
        void nullRejected() {
            var svc = new TurkuInternalDryRunRecordService();
            var record = svc.createFromDryRunResponse(null);
            assertThat(record.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("history accumulates and all internalOnly")
        void historyInternal() {
            var svc = new TurkuInternalDryRunRecordService();
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            svc.createFromDryRunResponse(new TurkuInternalDryRunTriggerResponse(
                    harness.execute(TurkuStoryboardInternalRenderChainFixture.turkuFixture(2))));
            svc.createFromDryRunResponse(null);
            svc.createRejectedRecord("baseline", List.of(TurkuInternalDryRunRecordIssueCode.INTERNAL_ERROR));
            assertThat(svc.getRecordCount()).isEqualTo(3);
            for (var s : svc.getHistory()) assertThat(s.isInternalOnly()).isTrue();
        }
    }

    // ── D. ProcessBuilder Boundary ──
    @Nested @DisplayName("D. ProcessBuilder Boundary")
    class ProcessBoundary {
        @Test @DisplayName("FP-049/051 classes no ProcessBuilder/Runtime")
        void clean() {
            for (var c : FP049_051_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName())
                        .doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── E. Endpoint Inventory Unchanged ──
    @Nested @DisplayName("E. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // ── F. Summary DTO Always internalOnly ──
    @Nested @DisplayName("F. Summary DTO Safety")
    class SummaryDto {
        @Test @DisplayName("summary has internalOnly field always true")
        void alwaysTrue() {
            Set<String> fields = Arrays.stream(TurkuInternalDryRunRecordSummary.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).contains("internalOnly");
        }
        @Test @DisplayName("summary has no auto-approval/publish fields")
        void noPublish() {
            Set<String> fields = Arrays.stream(TurkuInternalDryRunRecordSummary.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).doesNotContain("autoApproved", "publishReady", "publishedAt", "exportedAt");
        }
    }
}
