# FP-022 Task 03 — Render Execution Safety + Stub Runner Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-022 Task 03 — Render Execution Safety + Stub Runner |
| **Status** | ✅ COMPLETED |
| **Endpoints Added** | 2 (1 POST + 1 GET) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |

---

## Endpoints Added

| Method | Path | Description |
|--------|------|------------|
| POST | `/admin/turku-video-projects/{id}/run-render-job` | Execute stub render |
| GET | `/admin/turku-video-projects/{id}/render-execution-safety` | Read execution safety |

---

## Domain Models Added

### TurkuRenderExecutionPlan
Execution plan metadata: sceneCount, totalDuration, targetResolution, targetFps, targetFormat, estimatedRenderTime, rendererAlias, renderMode.

### TurkuRenderExecutionResult
Execution result: success, durationMs, failureCode, failureMessage, artifact, safeTraceRef.

### TurkuRenderExecutionSafetyEnvelope
Safety envelope: humanReviewRequired (true), copyrightReviewRequired (true), autoApproved (false), lyricsIncluded (false), copyrightClearanceVerified (false), renderMode, safetyFlags, reviewFlags, failureCodes, blockedReasons.

### TurkuRenderRunnerGateway (interface)
Gateway interface for render execution. Single method: `execute(plan) → result`.

### StubTurkuRenderRunnerGateway
Deterministic stub implementation. Produces safe internal metadata only. No real rendering. No binary. No FFmpeg. No cloud.

### TurkuRenderExecutionRequest/Response
Request: renderMode field. Response: projectId, jobId, attemptId, status, safeTraceRef, artifactRef, safety flags, failureCode.

### TurkuRenderExecutionSafetyResponse
Safety status: projectId, jobId, jobStatus, currentStage, safetyEnvelope, summary.

---

## Stub Runner Summary

- Default renderer: StubTurkuRenderRunnerGateway
- Produces deterministic metadata: opaque artifact ref, duration, format, resolution
- Artifact ref format: `internal-stub-artifact:{uuid}`
- Trace ref format: `stub-trace:{uuid}`
- No binary file, no FFmpeg, no cloud, no real rendering
- Always succeeds (failure path tested via service exception handling)

---

## Execution Safety Envelope

- humanReviewRequired: always true
- copyrightReviewRequired: always true
- autoApproved: always false
- lyricsIncluded: always false
- copyrightClearanceVerified: always false
- Safety flags: NO_REAL_RENDER, NO_BINARY_OUTPUT, NO_PUBLIC_DELIVERY, NO_EXPORT
- Review flags: HUMAN_REVIEW_REQUIRED, COPYRIGHT_REVIEW_REQUIRED, NOT_PUBLISH_READY, NOT_EXPORT_READY, INTERNAL_ONLY

---

## Workflow Transitions

```
RENDER_JOB_PREPARED → RENDER_JOB_RUNNING → RENDER_JOB_COMPLETED_INTERNAL
                                          or RENDER_JOB_FAILED_SAFE
                    → INTERNAL_RENDER_ARTIFACT_READY (future)
```

Stage enum: 19 values (was 15). +4 new stages.

---

## Artifact Metadata Policy

- Opaque internal ref only: `internal-stub-artifact:{uuid}`
- No file path, storage key, public/signed/CDN URL
- No base64/binary data
- No downloadable/exportable artifact
- humanReviewRequired=true, autoApproved=false on every artifact

---

## Actor Boundary Summary

- SYSTEM blocked on POST run-render-job
- GET render-execution-safety is unprivileged read-only
- actorRole TEST_FALLBACK compatibility only

---

## Safety Baseline Preserved

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No generic status update | ✅ |
| No provider/storage/raw/base64/secret exposure | ✅ |
| No unsafe HTML | ✅ |
| No token/JWT/session/principal/authorizationHeader | ✅ |
| No copyrighted lyrics/audio/video reproduction | ✅ |
| No rendered video file | ✅ |
| No downloadable file | ✅ |
| No FFmpeg/command/secret leakage | ✅ |

---

## Deferred: Task 04

Task 04 — Admin UI Render Job / Execution Safety Panel
