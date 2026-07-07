package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-032: Actual local smoke execution evidence service.
 * Coordinates availability check → manual execution → evidence result.
 * No ProcessBuilder in THIS class (delegates to checker and runner).
 */
public class TurkuFfmpegActualRunEvidenceService {

    private final TurkuFfmpegLocalAvailabilityChecker checker;
    private final TurkuFfmpegManualExecutionService manualService;

    public TurkuFfmpegActualRunEvidenceService(
            TurkuFfmpegLocalAvailabilityChecker checker,
            TurkuFfmpegManualExecutionService manualService) {
        this.checker = checker != null ? checker : new TurkuFfmpegLocalAvailabilityChecker();
        this.manualService = manualService != null ? manualService : new TurkuFfmpegManualExecutionService();
    }

    public TurkuFfmpegActualRunEvidenceService() {
        this(new TurkuFfmpegLocalAvailabilityChecker(), new TurkuFfmpegManualExecutionService());
    }

    /**
     * Attempt actual local smoke run with evidence.
     */
    public TurkuFfmpegActualRunEvidenceResult attempt(
            TurkuFfmpegManualExecutionRequest request,
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain,
            TurkuInternalRenderReviewState reviewState,
            TurkuFfmpegBinaryPolicy binaryPolicy,
            TurkuFfmpegExecutionEnvironment environment) {

        // 1. Check availability
        var availability = checker.check();
        return switch (availability) {
            case DISABLED -> TurkuFfmpegActualRunEvidenceResult.disabled();
            case UNSUPPORTED_ENVIRONMENT -> TurkuFfmpegActualRunEvidenceResult.unsupportedEnv();
            case UNAVAILABLE -> TurkuFfmpegActualRunEvidenceResult.unavailable();
            case AVAILABLE -> executeWithEvidence(request, readiness, safetyChain,
                    reviewState, binaryPolicy, environment);
        };
    }

    private TurkuFfmpegActualRunEvidenceResult executeWithEvidence(
            TurkuFfmpegManualExecutionRequest request,
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain,
            TurkuInternalRenderReviewState reviewState,
            TurkuFfmpegBinaryPolicy binaryPolicy,
            TurkuFfmpegExecutionEnvironment environment) {

        var result = manualService.execute(request, readiness, safetyChain,
                reviewState, binaryPolicy, environment);

        return switch (result.getStatus()) {
            case EXECUTION_COMPLETED_SAFE, VERIFICATION_PASSED -> {
                var metadata = result.getFixtureMetadata();
                yield TurkuFfmpegActualRunEvidenceResult.succeeded(
                        result.getOutputRef(), metadata, 0);
            }
            case EXECUTION_SKIPPED_FAKE ->
                TurkuFfmpegActualRunEvidenceResult.disabled();
            case EXECUTION_FAILED_SAFE ->
                TurkuFfmpegActualRunEvidenceResult.failedSafe();
            case BINARY_UNAVAILABLE ->
                TurkuFfmpegActualRunEvidenceResult.unavailable();
            case GATE_DENIED, ENVIRONMENT_DENIED, READINESS_DENIED, REVIEW_DENIED ->
                TurkuFfmpegActualRunEvidenceResult.gateDenied();
            default -> TurkuFfmpegActualRunEvidenceResult.gateDenied();
        };
    }
}
