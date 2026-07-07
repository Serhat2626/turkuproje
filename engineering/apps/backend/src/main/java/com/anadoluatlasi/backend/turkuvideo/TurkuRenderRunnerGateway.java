package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-022 Task 03: Gateway interface for render execution.
 * Implementations may be stub or real renderer adapters.
 */
public interface TurkuRenderRunnerGateway {
    TurkuRenderExecutionResult execute(TurkuRenderExecutionPlan plan);
}
