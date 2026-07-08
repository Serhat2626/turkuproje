package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-036: Internal render bundle manifest. Metadata-only.
 * No path/command/binary/provider/storage/URL exposure.
 */
public final class TurkuRenderBundleManifest {
    private final String opaqueBundleRef;
    private final TurkuRenderBundleStatus status;
    private final List<TurkuRenderBundleArtifactRef> artifacts;
    private final List<TurkuRenderBundleIssueCode> issueCodes;
    private final int artifactCount;
    private final int verifiedArtifactCount;
    private final int rejectedArtifactCount;
    private final long totalSizeBytes;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;

    public TurkuRenderBundleManifest(String opaqueBundleRef, TurkuRenderBundleStatus status,
            List<TurkuRenderBundleArtifactRef> artifacts, List<TurkuRenderBundleIssueCode> issues) {
        this.opaqueBundleRef = opaqueBundleRef;
        this.status = status;
        this.artifacts = artifacts != null ? List.copyOf(artifacts) : List.of();
        this.issueCodes = issues != null ? List.copyOf(issues) : List.of();
        this.artifactCount = this.artifacts.size();
        this.verifiedArtifactCount = (int) this.artifacts.stream().filter(TurkuRenderBundleArtifactRef::isVerified).count();
        this.rejectedArtifactCount = (int) this.artifacts.stream().filter(TurkuRenderBundleArtifactRef::isRejected).count();
        this.totalSizeBytes = this.artifacts.stream().mapToLong(TurkuRenderBundleArtifactRef::getSizeBytes).sum();
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
    }

    public String getOpaqueBundleRef() { return opaqueBundleRef; }
    public TurkuRenderBundleStatus getStatus() { return status; }
    public List<TurkuRenderBundleArtifactRef> getArtifacts() { return artifacts; }
    public List<TurkuRenderBundleIssueCode> getIssueCodes() { return issueCodes; }
    public int getArtifactCount() { return artifactCount; }
    public int getVerifiedArtifactCount() { return verifiedArtifactCount; }
    public int getRejectedArtifactCount() { return rejectedArtifactCount; }
    public long getTotalSizeBytes() { return totalSizeBytes; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }

    @Override
    public String toString() {
        return "BundleManifest{ref=" + opaqueBundleRef
                + ", status=" + status
                + ", artifacts=" + artifactCount
                + ", verified=" + verifiedArtifactCount
                + ", rejected=" + rejectedArtifactCount
                + ", issues=" + issueCodes + "}";
    }
}
