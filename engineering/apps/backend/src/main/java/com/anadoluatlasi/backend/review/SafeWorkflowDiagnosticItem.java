package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-017 Task 02: Safe immutable DTO for a single workflow diagnostic item.
 *
 * <p>Represents an internal workflow item that needs admin attention.
 * Read-only, informational — does NOT trigger any action.</p>
 *
 * <p>Safe fields only — no forbidden exposure.</p>
 */
@Getter
@Builder
public class SafeWorkflowDiagnosticItem {

    private final SafeWorkflowDiagnosticType diagnosticType;
    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final SafePipelineStage pipelineStage;
    private final ReviewState reviewState;
    private final String safeReasonCode;
    private final String safeMessage;
    private final List<SafeOperationalControlHint> safeRecommendedHints;
    private final LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "SafeWorkflowDiagnosticItem{"
                + "diagnosticType=" + diagnosticType
                + ", targetType=" + targetType
                + ", targetId=" + targetId
                + ", pipelineStage=" + pipelineStage
                + ", reviewState=" + reviewState
                + ", safeReasonCode='" + safeReasonCode + '\''
                + ", safeMessage='" + safeMessage + '\''
                + ", safeRecommendedHints=" + safeRecommendedHints
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
