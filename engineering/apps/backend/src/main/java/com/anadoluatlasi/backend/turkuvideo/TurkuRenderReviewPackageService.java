package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;

/**
 * FP-042: Service to build internal render review packages from safe metadata.
 * No auto-approval. No public delivery readiness.
 */
public class TurkuRenderReviewPackageService {

    private static final int MAX_ARTIFACTS = 10;

    /**
     * Build review package from verified artifact summaries.
     */
    public TurkuRenderReviewPackage build(String opaqueJobRef, String opaqueBundleRef,
            List<TurkuRenderReviewArtifactSummary> summaries,
            int sceneCount, int inputClipCount) {

        String pkgRef = "review-pkg:" + UUID.randomUUID();
        var issues = new ArrayList<TurkuRenderReviewPackageIssueCode>();
        var safe = summaries != null ? summaries : List.<TurkuRenderReviewArtifactSummary>of();

        // Validate
        if (safe.isEmpty()) {
            issues.add(TurkuRenderReviewPackageIssueCode.MISSING_ARTIFACT_REF);
        }
        if (safe.size() > MAX_ARTIFACTS) {
            issues.add(TurkuRenderReviewPackageIssueCode.TOO_MANY_ARTIFACTS);
        }

        boolean hasPrimary = false;
        var refSet = new HashSet<String>();
        for (var s : safe) {
            if (s.getOpaqueArtifactRef() == null || s.getOpaqueArtifactRef().isBlank())
                issues.add(TurkuRenderReviewPackageIssueCode.MISSING_ARTIFACT_REF);
            if (!refSet.add(s.getOpaqueArtifactRef()))
                issues.add(TurkuRenderReviewPackageIssueCode.DUPLICATE_ARTIFACT_REF);
            if (s.isPrimary()) hasPrimary = true;
            if (s.getVerificationStatus() == TurkuRenderArtifactVerificationStatus.REJECTED_DURATION
                    || s.getVerificationStatus() == TurkuRenderArtifactVerificationStatus.REJECTED_SIZE
                    || s.getVerificationStatus() == TurkuRenderArtifactVerificationStatus.REJECTED_MULTIPLE)
                issues.add(TurkuRenderReviewPackageIssueCode.ARTIFACT_REJECTED);
            if (!s.isVerified() && s.getVerificationStatus() != null
                    && s.getVerificationStatus() != TurkuRenderArtifactVerificationStatus.PASSED)
                issues.add(TurkuRenderReviewPackageIssueCode.ARTIFACT_NOT_VERIFIED);
            if (s.getRole() == null)
                issues.add(TurkuRenderReviewPackageIssueCode.ARTIFACT_ROLE_UNKNOWN);
        }
        if (!hasPrimary && !safe.isEmpty())
            issues.add(TurkuRenderReviewPackageIssueCode.NO_PRIMARY_ARTIFACT);

        // Compute aggregates
        int totalDuration = safe.stream().mapToInt(TurkuRenderReviewArtifactSummary::getDurationSeconds).sum();
        int maxW = safe.stream().mapToInt(TurkuRenderReviewArtifactSummary::getWidth).max().orElse(0);
        int maxH = safe.stream().mapToInt(TurkuRenderReviewArtifactSummary::getHeight).max().orElse(0);
        int maxFps = safe.stream().mapToInt(TurkuRenderReviewArtifactSummary::getFps).max().orElse(0);
        long totalSize = safe.stream().mapToLong(TurkuRenderReviewArtifactSummary::getSizeBytes).sum();

        TurkuRenderReviewPackageStatus status;
        if (!issues.isEmpty()) {
            boolean hasRejection = issues.stream().anyMatch(i ->
                    i == TurkuRenderReviewPackageIssueCode.ARTIFACT_REJECTED
                    || i == TurkuRenderReviewPackageIssueCode.ARTIFACT_NOT_VERIFIED);
            status = hasRejection
                    ? TurkuRenderReviewPackageStatus.REJECTED_UNSAFE
                    : TurkuRenderReviewPackageStatus.FAILED_SAFE;
        } else {
            status = TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY;
        }

        return new TurkuRenderReviewPackage(pkgRef, opaqueJobRef, opaqueBundleRef,
                status, issues, safe, sceneCount, inputClipCount,
                totalDuration, maxW, maxH, maxFps, totalSize);
    }
}
