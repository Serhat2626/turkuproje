package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-024 Task 04: Controlled FFmpeg execution gateway.
 * Disabled by default. Supports dry-run and simulated execution.
 * No ProcessBuilder. No Runtime.exec. No filesystem access.
 */
public class TurkuFfmpegExecutionGateway {

    private final TurkuRenderConfigValidator configValidator;
    private final TurkuRenderWorkspaceValidator workspaceValidator;

    public TurkuFfmpegExecutionGateway() {
        this.configValidator = new TurkuRenderConfigValidator();
        this.workspaceValidator = new TurkuRenderWorkspaceValidator();
    }

    /**
     * Execute in controlled mode. Never starts a real process.
     * Validates config → workspace → command plan → returns safe result.
     */
    public TurkuFfmpegExecutionResult execute(TurkuFfmpegExecutionRequest request) {
        if (request == null) {
            return TurkuFfmpegExecutionResult.failedSafe(
                    TurkuFfmpegExecutionFailureCode.GENERAL_FAILURE,
                    "Execution request is null");
        }

        // Step 1: Validate config
        var configResult = configValidator.validate(request.getConfig());
        if (configResult.getResolvedMode() == TurkuRenderConfigMode.FFMPEG_CONFIG_MISSING) {
            return TurkuFfmpegExecutionResult.configMissing();
        }
        if (configResult.getResolvedMode() == TurkuRenderConfigMode.FFMPEG_DISABLED) {
            return TurkuFfmpegExecutionResult.disabled();
        }
        if (!configResult.isValid()) {
            List<String> notes = configResult.getIssues().stream()
                    .map(TurkuRenderConfigValidationIssue::getSafeMessage)
                    .toList();
            return TurkuFfmpegExecutionResult.configInvalid(notes);
        }

        // Step 2: Validate workspace ref
        var wsResult = workspaceValidator.validateWorkspaceRef(request.getWorkspaceRef());
        if (!wsResult.isValid()) {
            return TurkuFfmpegExecutionResult.dryRunFailed(
                    TurkuFfmpegExecutionFailureCode.WORKSPACE_INVALID,
                    "Workspace ref validation failed",
                    wsResult.getIssues().stream()
                            .map(TurkuRenderWorkspaceIssue::getSafeMessage).toList());
        }

        // Step 3: Validate command plan
        if (request.getCommandPlan() == null) {
            return TurkuFfmpegExecutionResult.dryRunFailed(
                    TurkuFfmpegExecutionFailureCode.COMMAND_PLAN_INVALID,
                    "Command plan is null", List.of());
        }
        if (!request.getCommandPlan().isValidated()) {
            return TurkuFfmpegExecutionResult.dryRunFailed(
                    TurkuFfmpegExecutionFailureCode.COMMAND_PLAN_INVALID,
                    "Command plan not validated", List.of());
        }

        // Step 4: Check command plan for unsafe values
        if (request.getCommandPlan().getSafeArgs() != null) {
            for (String arg : request.getCommandPlan().getSafeArgs()) {
                if (workspaceValidator.isShellMeta(arg)) {
                    return TurkuFfmpegExecutionResult.dryRunFailed(
                            TurkuFfmpegExecutionFailureCode.COMMAND_PLAN_UNSAFE,
                            "Unsafe value in command plan args",
                            List.of("Shell metacharacter detected"));
                }
            }
        }

        // Step 5: Validate limits from render plan
        if (request.getRenderPlan() != null) {
            var plan = request.getRenderPlan();
            if (plan.getTotalDurationSeconds() > request.getConfig().getMaxDurationSeconds()) {
                return TurkuFfmpegExecutionResult.dryRunFailed(
                        TurkuFfmpegExecutionFailureCode.LIMITS_EXCEEDED,
                        "Duration exceeds config limit", List.of());
            }
        }

        // Step 6: Dry-run success — all validation passed, no real execution
        long startMs = System.currentTimeMillis();
        List<String> notes = new ArrayList<>();
        notes.add("Config: valid");
        notes.add("Workspace: valid");
        notes.add("Command plan: validated");
        notes.add("Limits: within bounds");
        notes.add("Mode: DRY_RUN — no real execution");
        long elapsed = System.currentTimeMillis() - startMs;

        return TurkuFfmpegExecutionResult.dryRunSuccess(notes, elapsed);
    }

    /**
     * Simulated execution — returns success with opaque artifact ref.
     * No real process, no real file.
     */
    public TurkuFfmpegExecutionResult simulatedExecution(TurkuFfmpegExecutionRequest request) {
        if (request == null) {
            return TurkuFfmpegExecutionResult.failedSafe(
                    TurkuFfmpegExecutionFailureCode.GENERAL_FAILURE,
                    "Request is null");
        }
        // Validate first
        var dryResult = execute(request);
        if (!dryResult.isSuccess()) {
            return dryResult;
        }
        // Simulated success
        return TurkuFfmpegExecutionResult.simulatedSuccess(1);
    }

    /**
     * Returns the current execution mode (always DISABLED in this implementation).
     */
    public TurkuFfmpegExecutionMode getDefaultMode() {
        return TurkuFfmpegExecutionMode.DISABLED;
    }
}
