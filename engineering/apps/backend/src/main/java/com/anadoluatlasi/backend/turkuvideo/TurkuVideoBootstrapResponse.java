package com.anadoluatlasi.backend.turkuvideo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-019 Task 02: Bootstrap response DTO. Safe fields only.
 */
public final class TurkuVideoBootstrapResponse {
    private final UUID id;
    private final String turkuName;
    private final TurkuVideoWorkflowStage workflowStage;
    private final LocalDateTime createdAt;
    private final String safeMessage;

    public TurkuVideoBootstrapResponse(UUID id, String turkuName,
                                        TurkuVideoWorkflowStage workflowStage,
                                        LocalDateTime createdAt, String safeMessage) {
        this.id = id;
        this.turkuName = turkuName;
        this.workflowStage = workflowStage;
        this.createdAt = createdAt;
        this.safeMessage = safeMessage;
    }

    public UUID getId() { return id; }
    public String getTurkuName() { return turkuName; }
    public TurkuVideoWorkflowStage getWorkflowStage() { return workflowStage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getSafeMessage() { return safeMessage; }

    public static TurkuVideoBootstrapResponse from(TurkuVideoProject project) {
        return new TurkuVideoBootstrapResponse(
                project.getId(), project.getTurkuName(),
                project.getWorkflowStage(), project.getCreatedAt(),
                "Türkü video projesi başarıyla oluşturuldu");
    }
}
