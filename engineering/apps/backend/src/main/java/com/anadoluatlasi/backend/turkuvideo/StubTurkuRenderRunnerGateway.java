package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-022 Task 03: Deterministic stub renderer.
 * Produces safe internal metadata only. No real rendering.
 * No binary file. No FFmpeg. No cloud execution.
 */
public class StubTurkuRenderRunnerGateway implements TurkuRenderRunnerGateway {

    @Override
    public TurkuRenderExecutionResult execute(TurkuRenderExecutionPlan plan) {
        TurkuRenderArtifact artifact = new TurkuRenderArtifact(
                UUID.randomUUID(),
                "internal-stub-artifact:" + UUID.randomUUID(),
                plan.getTargetFormat(),
                plan.getTotalDurationSeconds(),
                plan.getTargetResolution(),
                "estimated: ~" + Math.round(plan.getTotalDurationSeconds() * 2) + "MB");

        return new TurkuRenderExecutionResult(
                true, 42L, null, null, artifact,
                "stub-trace:" + UUID.randomUUID());
    }
}
