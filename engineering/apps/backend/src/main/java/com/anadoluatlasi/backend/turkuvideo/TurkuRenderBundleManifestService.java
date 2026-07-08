package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-036: Render bundle manifest service.
 * Builds/validates bundle from safe artifact refs. No ProcessBuilder.
 */
public class TurkuRenderBundleManifestService {

    private static final int MAX_ARTIFACTS = 10;
    private static final long MAX_TOTAL_SIZE_BYTES = 100L * 1024 * 1024; // 100MB

    /**
     * Build manifest from artifact refs with policy validation.
     */
    public TurkuRenderBundleManifest build(String opaqueBundleRef,
            List<TurkuRenderBundleArtifactRef> artifacts) {

        var issues = new ArrayList<TurkuRenderBundleIssueCode>();
        var safeArtifacts = artifacts != null ? artifacts : List.<TurkuRenderBundleArtifactRef>of();

        // 1. Bundle ref
        if (opaqueBundleRef == null || opaqueBundleRef.isBlank()) {
            opaqueBundleRef = "bundle:" + UUID.randomUUID();
            issues.add(TurkuRenderBundleIssueCode.BUNDLE_REF_MISSING);
        }

        // 2. Max artifacts
        if (safeArtifacts.size() > MAX_ARTIFACTS)
            issues.add(TurkuRenderBundleIssueCode.TOO_MANY_ARTIFACTS);

        // 3. Total size
        long totalSize = safeArtifacts.stream().mapToLong(TurkuRenderBundleArtifactRef::getSizeBytes).sum();
        if (totalSize > MAX_TOTAL_SIZE_BYTES)
            issues.add(TurkuRenderBundleIssueCode.TOTAL_SIZE_EXCEEDS_LIMIT);

        // 4. Primary video check
        long primaryCount = safeArtifacts.stream()
                .filter(a -> a.getRole() == TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP).count();
        if (primaryCount == 0)
            issues.add(TurkuRenderBundleIssueCode.MISSING_PRIMARY_VIDEO);
        else if (primaryCount > 1)
            issues.add(TurkuRenderBundleIssueCode.DUPLICATE_PRIMARY_VIDEO);

        // 5. Role validation
        if (safeArtifacts.stream().anyMatch(a -> !a.getRole().isAllowed()))
            issues.add(TurkuRenderBundleIssueCode.UNSUPPORTED_ROLE);

        // 6. Artifact status checks
        if (safeArtifacts.stream().anyMatch(TurkuRenderBundleArtifactRef::isRejected))
            issues.add(TurkuRenderBundleIssueCode.ARTIFACT_REJECTED);
        if (safeArtifacts.stream().anyMatch(a ->
                a.getVerificationStatus() == TurkuRenderArtifactVerificationStatus.FAILED_SAFE))
            issues.add(TurkuRenderBundleIssueCode.ARTIFACT_FAILED);

        // 7. Determine bundle status
        TurkuRenderBundleStatus status;
        if (issues.stream().anyMatch(i -> i == TurkuRenderBundleIssueCode.ARTIFACT_FAILED))
            status = TurkuRenderBundleStatus.FAILED_SAFE;
        else if (issues.stream().anyMatch(i -> i == TurkuRenderBundleIssueCode.ARTIFACT_REJECTED
                || i == TurkuRenderBundleIssueCode.UNSUPPORTED_ROLE
                || i == TurkuRenderBundleIssueCode.TOO_MANY_ARTIFACTS
                || i == TurkuRenderBundleIssueCode.TOTAL_SIZE_EXCEEDS_LIMIT
                || i == TurkuRenderBundleIssueCode.MISSING_PRIMARY_VIDEO
                || i == TurkuRenderBundleIssueCode.DUPLICATE_PRIMARY_VIDEO))
            status = TurkuRenderBundleStatus.REJECTED_UNSAFE;
        else if (safeArtifacts.stream().allMatch(TurkuRenderBundleArtifactRef::isVerified))
            status = TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY;
        else
            status = TurkuRenderBundleStatus.ARTIFACTS_ATTACHED;

        return new TurkuRenderBundleManifest(opaqueBundleRef, status, safeArtifacts, issues);
    }
}
