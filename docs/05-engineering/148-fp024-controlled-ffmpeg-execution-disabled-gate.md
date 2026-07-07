# FP-024 Task 04 — Controlled FFmpeg Execution Behind Disabled-by-Default Gate

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-024 Task 04 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |
| **Real FFmpeg Execution** | None |
| **ProcessBuilder** | None |
| **Filesystem Access** | None |

---

## Execution Mode Summary

### TurkuFfmpegExecutionMode (6 values)

| Mode | Meaning |
|------|---------|
| DISABLED | FFmpeg execution disabled (default) |
| CONFIG_MISSING | No config present |
| CONFIG_INVALID | Config present but fails validation |
| DRY_RUN | All validation passed, no real execution |
| SIMULATED_SUCCESS | Simulated execution returned opaque result |
| FAILED_SAFE | Execution failed safely |

### TurkuFfmpegExecutionFailureCode (13 values)

EXECUTION_DISABLED, CONFIG_MISSING, CONFIG_INVALID, WORKSPACE_INVALID, FILE_REF_INVALID, COMMAND_PLAN_INVALID, COMMAND_PLAN_UNSAFE, TIMEOUT_EXCEEDED, LIMITS_EXCEEDED, DRY_RUN_FAILED, SIMULATED_FAILURE, SAFETY_VIOLATION, GENERAL_FAILURE

---

## Disabled-by-Default Behavior

- `TurkuFfmpegExecutionGateway.getDefaultMode()` → DISABLED
- null config → CONFIG_MISSING
- disabled config → DISABLED
- invalid config → CONFIG_INVALID with safe validation notes
- No real process ever started

---

## Dry-Run / Simulated Execution Boundary

### Dry-Run
Validates config → workspace → command plan → limits → returns DRY_RUN result with opaque artifact ref. No real execution.

### Simulated Execution
Runs dry-run first. If passes, returns SIMULATED_SUCCESS with opaque artifact ref. No real execution.

### Validation Chain
1. Config validation (TurkuRenderConfigValidator)
2. Workspace ref validation (TurkuRenderWorkspaceValidator)
3. Command plan null/validated check
4. Command plan args shell metacharacter check
5. Duration limit check against config bounds

---

## No-Process / No-Filesystem Policy

- No ProcessBuilder field in any class
- No Process, ProcessHandle, Runtime field
- No File, Path, Files API usage
- No real file creation/deletion
- No storage provider access
- All results contain only opaque refs and safe metadata

---

## Integration Summary

- Gateway uses existing TurkuRenderConfigValidator and TurkuRenderWorkspaceValidator
- StubTurkuRenderRunnerGateway remains default via TurkuRenderRunnerResolver
- Existing run-render-job endpoint behavior unchanged
- FFmpeg execution gateway is testable standalone, not wired into controller

---

## Safety Baseline Preserved

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No unsafe HTML | ✅ |
| No token/JWT/session exposure | ✅ |
| humanReviewRequired=true | ✅ |
| autoApproved=false | ✅ |
| NO_PROCESS_EXECUTION in safety flags | ✅ |
| 6 classes × 29 forbidden fields = 0 violations | ✅ |

---

## Deferred: Task 05

Task 05 — Internal Artifact Verification + Metadata Integration.
