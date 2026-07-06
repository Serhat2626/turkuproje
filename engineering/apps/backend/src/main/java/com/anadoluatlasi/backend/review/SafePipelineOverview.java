package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FP-016 Task 02: Safe immutable DTO for the full pipeline overview.
 *
 * <p>Wraps a list of {@link SafePipelineAggregateItem} with aggregate
 * counts by pipeline stage and target type.</p>
 *
 * <p>This DTO is internal-only. It does NOT imply publication, export,
 * or public delivery eligibility.</p>
 *
 * <p>Safe fields only — no forbidden exposure.</p>
 */
@Getter
@Builder
public class SafePipelineOverview {

    private final List<SafePipelineAggregateItem> items;
    private final int totalCount;
    private final Map<SafePipelineStage, Integer> countsByStage;
    private final Map<ReviewTargetType, Integer> countsByTargetType;
    private final List<String> safeWarnings;
    private final LocalDateTime updatedAt;

    /**
     * Factory for an empty overview (no items found).
     */
    public static SafePipelineOverview empty() {
        return SafePipelineOverview.builder()
                .items(List.of())
                .totalCount(0)
                .countsByStage(Map.of())
                .countsByTargetType(Map.of())
                .safeWarnings(List.of())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Safe toString — no forbidden fields.
     */
    @Override
    public String toString() {
        return "SafePipelineOverview{"
                + "totalCount=" + totalCount
                + ", countsByStage=" + countsByStage
                + ", countsByTargetType=" + countsByTargetType
                + ", warningCount=" + (safeWarnings != null ? safeWarnings.size() : 0)
                + '}';
    }
}
