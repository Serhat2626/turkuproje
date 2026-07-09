package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LIVE-005 — Human Review + Controlled Delivery + Final Closure")
class TurkuLiveReviewDeliveryTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt",
            "command", "args", "processBuilder");

    private static final List<Class<?>> LIVE005_CLASSES = List.of(
            TurkuLiveReviewChecklistItem.class, TurkuLiveControlledDeliveryResult.class,
            TurkuLiveReviewDeliveryService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "RENDERED", "GENERATED", "DELIVERED", "AUTO_APPROVED", "AUTO_PUBLISHED");

    private TurkuLiveReviewDeliveryService service;

    @BeforeEach void setUp() { service = new TurkuLiveReviewDeliveryService(); }

    private TurkuLiveRenderResult fakeRender() {
        var req = new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("Test", "Test", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        var e = new TurkuManualEnablementService().evaluate(r);
        var wo = new TurkuWorkOrderPreviewService().generate(e);
        var gate = new TurkuExecutionGateService().evaluate(wo);
        var prov = new TurkuProviderConfigService().evaluate(gate);
        var render = new TurkuRenderPipelineConfigService().evaluate(prov);
        var exec = new TurkuExecutionPackageService().assemble(wo, prov, render);
        var prompt = new TurkuImagePromptPackageService().prepare(exec);
        var gen = new TurkuLiveImageGenerationService().executeFake(prompt);
        var storage = new TurkuLiveArtifactStorageService().storeFake(gen);
        return new TurkuLiveRenderExecutionService().renderFake(storage);
    }

    // A. Pending Review Blocks Delivery
    @Nested @DisplayName("A. Pending Review Blocks Delivery")
    class PendingBlocks {
        @Test @DisplayName("null review -> PENDING_HUMAN_REVIEW")
        void nullReview() {
            var r = service.evaluate(fakeRender(), null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveDeliveryStatus.PENDING_HUMAN_REVIEW);
            assertThat(r.isExportEligible()).isFalse();
        }
        @Test @DisplayName("PENDING -> blocked")
        void pendingReview() {
            var r = service.evaluate(fakeRender(), TurkuLiveReviewDecision.PENDING, null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveDeliveryStatus.PENDING_HUMAN_REVIEW);
            assertThat(r.isExportEligible()).isFalse();
        }
    }

    // B. Rejected Blocks Delivery
    @Nested @DisplayName("B. Rejected Blocks Delivery")
    class RejectedBlocks {
        @Test @DisplayName("REJECTED -> BLOCKED_BY_REVIEW")
        void rejected() {
            var r = service.evaluate(fakeRender(), TurkuLiveReviewDecision.REJECTED, null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveDeliveryStatus.BLOCKED_BY_REVIEW);
            assertThat(r.isExportEligible()).isFalse();
            assertThat(r.getIssues()).contains(TurkuLiveDeliveryIssueCode.REVIEW_REJECTED);
        }
    }

    // C. Needs Revision Blocks
    @Nested @DisplayName("C. Needs Revision Blocks")
    class RevisionBlocks {
        @Test @DisplayName("NEEDS_REVISION -> BLOCKED_BY_REVIEW")
        void revision() {
            var r = service.evaluate(fakeRender(), TurkuLiveReviewDecision.NEEDS_REVISION, null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveDeliveryStatus.BLOCKED_BY_REVIEW);
            assertThat(r.isExportEligible()).isFalse();
        }
    }

    // D. Approved -> Controlled Delivery
    @Nested @DisplayName("D. Approved -> Controlled Delivery")
    class Approved {
        @Test @DisplayName("ELIGIBLE_FOR_CONTROLLED_DELIVERY")
        void eligible() {
            var r = service.evaluate(fakeRender(), TurkuLiveReviewDecision.APPROVED_FOR_CONTROLLED_DELIVERY, null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveDeliveryStatus.ELIGIBLE_FOR_CONTROLLED_DELIVERY);
            assertThat(r.isExportEligible()).isTrue();
            assertThat(r.getOpaqueDeliveryRef()).startsWith("ldlv:");
            assertThat(r.getReviewDecision()).isEqualTo(TurkuLiveReviewDecision.APPROVED_FOR_CONTROLLED_DELIVERY);
        }
        @Test @DisplayName("export note mentions admin only")
        void adminOnly() {
            var r = service.evaluate(fakeRender(), TurkuLiveReviewDecision.APPROVED_FOR_CONTROLLED_DELIVERY, null);
            assertThat(r.getExportEligibilityNote()).contains("admin");
        }
    }

    // E. No Render -> NOT_ELIGIBLE
    @Nested @DisplayName("E. No Render -> NOT_ELIGIBLE")
    class NoRender {
        @Test @DisplayName("null render -> NOT_ELIGIBLE")
        void nullRender() {
            var r = service.evaluate(null, TurkuLiveReviewDecision.APPROVED_FOR_CONTROLLED_DELIVERY, null);
            assertThat(r.getStatus()).isEqualTo(TurkuLiveDeliveryStatus.NOT_ELIGIBLE);
        }
    }

    // F. Review Checklist
    @Nested @DisplayName("F. Review Checklist")
    class Checklist {
        @Test @DisplayName("default checklist has 6 items")
        void defaultItems() {
            assertThat(TurkuLiveReviewChecklistItem.defaultChecklist()).hasSize(6);
        }
        @Test @DisplayName("all default items unchecked")
        void unchecked() {
            for (var item : TurkuLiveReviewChecklistItem.defaultChecklist())
                assertThat(item.isChecked()).isFalse();
        }
    }

    // G. Forbidden Sweep
    @Nested @DisplayName("G. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : LIVE005_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // H. No Forbidden Statuses
    @Nested @DisplayName("H. No Forbidden Statuses")
    class NoForbidden {
        @Test @DisplayName("delivery status safe")
        void safe() {
            for (var v : TurkuLiveDeliveryStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("review decision safe")
        void reviewSafe() {
            for (var v : TurkuLiveReviewDecision.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("6 delivery statuses")
        void count() { assertThat(TurkuLiveDeliveryStatus.values()).hasSize(6); }
        @Test @DisplayName("4 review decisions")
        void decisions() { assertThat(TurkuLiveReviewDecision.values()).hasSize(4); }
    }

    // I. Endpoint Unchanged
    @Nested @DisplayName("I. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total unchanged")
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
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // J. ProcessBuilder / Runtime
    @Nested @DisplayName("J. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : LIVE005_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
