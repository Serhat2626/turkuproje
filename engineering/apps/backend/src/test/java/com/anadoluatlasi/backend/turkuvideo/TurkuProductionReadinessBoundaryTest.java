package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-054 — Final Production Readiness Boundary + Admin Internal Operations Closure.
 * Dense hardening across FP-034→FP-053. No new capability.
 */
@DisplayName("FP-054 — Production Readiness Boundary")
class TurkuProductionReadinessBoundaryTest {

    private static final List<String> FORBIDDEN_FIELDS = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl", "storageProvider",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html", "mediaBody", "authHeader");

    /** All FP-034→FP-053 production-boundary classes */
    private static final List<Class<?>> ALL_BOUNDARY_CLASSES = List.of(
            // FP-037 scene render
            TurkuSceneRenderPlanStatus.class, TurkuSceneRenderPlan.class,
            // FP-038 multi-scene
            TurkuMultiSceneRenderJobStatus.class, TurkuMultiSceneRenderJobPlan.class,
            // FP-039 clip generation
            TurkuMultiClipGenerationStatus.class, TurkuMultiClipGenerationResult.class,
            // FP-040 stitching
            TurkuClipStitchingStatus.class, TurkuClipStitchingResult.class,
            // FP-041 lifecycle
            TurkuRenderArtifactLifecycleStatus.class, TurkuRenderArtifactLifecycleReport.class,
            // FP-042 bundle
            TurkuRenderBundleStatus.class, TurkuRenderBundleManifest.class,
            // FP-043 admin visibility
            TurkuInternalRenderReviewState.class, TurkuInternalRenderReviewSummary.class,
            // FP-044 dry run
            TurkuInternalStoryVideoDryRunStatus.class, TurkuInternalStoryVideoDryRunSummary.class,
            // FP-046 storyboard adapter
            TurkuStoryboardRenderAdapterStatus.class, TurkuStoryboardRenderPlanAdapterResult.class,
            // FP-047 storyboard chain
            TurkuStoryboardInternalRenderChainStatus.class, TurkuStoryboardInternalRenderChainFixture.class,
            // FP-049 trigger
            TurkuInternalDryRunTriggerRequest.class, TurkuInternalDryRunTriggerResponse.class,
            // FP-051 record
            TurkuInternalDryRunRecord.class, TurkuInternalDryRunRecordSummary.class,
            TurkuInternalDryRunRecordService.class,
            // FP-053 operational
            TurkuInternalDryRunOperationalSummary.class, TurkuInternalDryRunOperationalSummaryService.class);

    // ── A. Full Forbidden Field Sweep (27 classes) ──
    @Nested @DisplayName("A. Forbidden Field Sweep (FP-034→FP-053)")
    class ForbiddenSweep {
        @Test @DisplayName("all 27 boundary classes — 0 forbidden fields")
        void sweep() {
            for (var c : ALL_BOUNDARY_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN_FIELDS)
                    assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── B. No Publish/Export/Auto-Approved Statuses ──
    @Nested @DisplayName("B. No READY_FOR_PUBLISH / COMPLETED / AUTO_APPROVED")
    class NoPublishStatuses {
        @Test @DisplayName("all status enums clean")
        void allClean() {
            List<Class<? extends Enum<?>>> enums = List.of(
                    TurkuSceneRenderPlanStatus.class, TurkuMultiSceneRenderJobStatus.class,
                    TurkuMultiClipGenerationStatus.class, TurkuClipStitchingStatus.class,
                    TurkuRenderArtifactLifecycleStatus.class, TurkuRenderBundleStatus.class,
                    TurkuInternalRenderReviewState.class, TurkuInternalStoryVideoDryRunStatus.class,
                    TurkuStoryboardRenderAdapterStatus.class, TurkuStoryboardInternalRenderChainStatus.class,
                    TurkuInternalDryRunRecordStatus.class, TurkuInternalDryRunOperationalSummaryStatus.class);
            for (var e : enums) {
                for (var v : e.getEnumConstants()) {
                    assertThat(v.name()).as("%s.%s", e.getSimpleName(), v.name())
                            .doesNotContain("READY_FOR_PUBLISH")
                            .doesNotContain("COMPLETED")
                            .doesNotContain("AUTO_APPROVED")
                            .doesNotContain("EXPORTED")
                            .doesNotContain("PUBLISHED");
                }
            }
        }
    }

    // ── C. Endpoint Inventory ──
    @Nested @DisplayName("C. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class EndpointInventory {
        @Test @DisplayName("controller: 17 POST + 8 GET = 25")
        void controller() {
            long post = count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post).as("POST").isEqualTo(17);
            assertThat(get).as("GET").isEqualTo(8);
        }
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
            assertThat(post + get).as("total").isEqualTo(46);
            assertThat(post).as("POST").isEqualTo(26);
            assertThat(get).as("GET").isEqualTo(20);
        }
        @Test @DisplayName("all controller paths contain /admin")
        void adminOnly() {
            for (var c : List.of(AdminReviewController.class, AdminPipelineController.class,
                    AdminWorkflowObservabilityController.class, TurkuVideoProjectController.class)) {
                var mapping = c.getAnnotation(RequestMapping.class);
                assertThat(mapping).as(c.getSimpleName()).isNotNull();
                assertThat(mapping.value()[0]).as(c.getSimpleName()).contains("admin");
            }
        }
    }

    // ── D. ProcessBuilder Boundary ──
    @Nested @DisplayName("D. ProcessBuilder Boundary")
    class ProcessBoundary {
        @Test @DisplayName("no ProcessBuilder/Process/Runtime fields in boundary classes")
        void noProcess() {
            for (var c : ALL_BOUNDARY_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName())
                        .doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── E. Internal Chain Integration ──
    @Nested @DisplayName("E. Internal Chain Integration")
    class ChainIntegration {
        @Test @DisplayName("dry-run → record → summary chain works")
        void fullChain() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var chainResult = harness.execute(fixture);
            var triggerResponse = new TurkuInternalDryRunTriggerResponse(chainResult);
            var recordService = new TurkuInternalDryRunRecordService();
            var record = recordService.createFromDryRunResponse(triggerResponse);
            var summaryService = new TurkuInternalDryRunOperationalSummaryService();
            var opSummary = summaryService.summarize(recordService);

            assertThat(chainResult.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE);
            assertThat(triggerResponse.isInternalOnly()).isTrue();
            assertThat(record.getStatus()).isEqualTo(TurkuInternalDryRunRecordStatus.READY_FOR_ADMIN_HISTORY_ONLY);
            assertThat(opSummary.getOverallStatus()).isEqualTo(TurkuInternalDryRunOperationalSummaryStatus.ALL_ACCEPTED);
            assertThat(opSummary.isInternalOnly()).isTrue();
        }
        @Test @DisplayName("rejected chain → rejected record → rejected summary")
        void rejectedChain() {
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var chainResult = harness.execute(null);
            var triggerResponse = new TurkuInternalDryRunTriggerResponse(chainResult);
            var recordService = new TurkuInternalDryRunRecordService();
            recordService.createFromDryRunResponse(triggerResponse);
            var opSummary = new TurkuInternalDryRunOperationalSummaryService().summarize(recordService);

            assertThat(chainResult.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE);
            assertThat(opSummary.getOverallStatus()).isEqualTo(TurkuInternalDryRunOperationalSummaryStatus.ALL_REJECTED);
        }
    }

    // ── F. No Public Delivery / Generic Update ──
    @Nested @DisplayName("F. No Public Delivery / Generic Update")
    class NoPublicDelivery {
        @Test @DisplayName("TurkuVideoWorkflowStage has no PUBLIC/DELIVER/EXPORT/DOWNLOAD")
        void workflowStage() {
            for (var v : TurkuVideoWorkflowStage.values()) {
                assertThat(v.name()).doesNotContain("PUBLIC");
                assertThat(v.name()).doesNotContain("DELIVER");
                assertThat(v.name()).doesNotContain("EXPORT");
                assertThat(v.name()).doesNotContain("DOWNLOAD");
            }
        }
    }

    private static long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
        return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
    }
}
