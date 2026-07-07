# FP-022 Task 02 — Render Job Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-022 Task 02 — Render Job Backend |
| **Status** | ✅ COMPLETED |
| **Endpoints Added** | 2 (1 POST + 1 GET) |
| **Endpoint Inventory** | 42 = 24 POST + 18 GET |
| **Migration/schema/config** | None |

---

## Endpoints Added

| Method | Path | Description |
|--------|------|------------|
| POST | `/admin/turku-video-projects/{id}/prepare-render-job` | Create internal render job from manifest |
| GET | `/admin/turku-video-projects/{id}/render-job` | Read current render job status |

---

## Domain Models

### TurkuRenderJob
Internal render job aggregate. Fields: jobId, projectId, manifestRef, status, humanReviewRequired (true), copyrightReviewRequired (true), autoApproved (false), createdAt, updatedAt, attempts list, artifact, reviewFlags.

### TurkuRenderJobStatus
Enum: PREPARED, RUNNING, COMPLETED_INTERNAL, FAILED_SAFE, CANCELLED.
No PUBLISHED/EXPORTED/AUTO_APPROVED.

### TurkuRenderAttempt
Individual render attempt record: attemptId, attemptNo, startedAt, completedAt, status, failureCode, durationMs, rendererAlias.

### TurkuRenderArtifact
Opaque artifact metadata: artifactId, artifactRef (opaque internal ref), format, durationSeconds, resolution, sizeLabel, humanReviewRequired (true), autoApproved (false).
No file path, storage key, public/signed/CDN URL, base64/binary.

### TurkuRenderFailureCode
Enum: MANIFEST_INCOMPLETE, READINESS_CHECK_FAILED, RENDERER_UNAVAILABLE, RENDERER_TIMEOUT, RENDERER_INTERNAL_ERROR, SAFETY_VIOLATION, COPYRIGHT_REVIEW_PENDING, HUMAN_REVIEW_REQUIRED.

### TurkuRenderJobRequest
Empty placeholder — render mode and options deferred to Task 03.

### TurkuRenderJobResponse
Safe response DTO: projectId, jobId, status, currentStage, manifestRef, attemptCount, humanReviewRequired (true), copyrightReviewRequired (true), autoApproved (false), notPublishReady (true), notExportReady (true), reviewFlags, summary.

---

## Service: TurkuRenderJobService

### prepareRenderJob(projectId)
1. Validates project exists
2. Validates stage == RENDER_MANIFEST_PREPARED
3. Validates render manifest exists
4. Validates readiness is not BLOCKED
5. Creates TurkuRenderJob with PREPARED status
6. Sets review flags: HUMAN_REVIEW_REQUIRED, COPYRIGHT_REVIEW_REQUIRED, NOT_PUBLISH_READY, NOT_EXPORT_READY, INTERNAL_RENDER_ONLY
7. Stores render job on project
8. Advances stage → RENDER_JOB_PREPARED
9. Returns safe TurkuRenderJobResponse

### getRenderJob(projectId)
1. Validates project exists
2. Returns current render job status or safe empty state
3. No side effects

---

## Controller Updates

TurkuVideoProjectController constructor extended with TurkuRenderJobService (8th parameter).

POST endpoint uses `resolvePlanningActor()` — SYSTEM role blocked.
GET endpoint is read-only, no actor resolution needed.

---

## Workflow Transition

```
RENDER_MANIFEST_PREPARED → RENDER_JOB_PREPARED
```

Stage enum now has 15 values (was 14).

---

## Artifact Placeholder Policy

- Render job creates metadata only
- No actual artifact file produced
- manifestRef is opaque: `internal-manifest:{uuid}`
- No storage key, file path, public/signed/CDN URL
- No binary/base64 data in any DTO

---

## No-Render/No-Export/No-Download Policy

- No real video rendering in Task 02
- No FFmpeg or cloud integration
- No binary output in any DTO
- No downloadable file endpoint
- No export endpoint
- No publish endpoint

---

## Actor Boundary Summary

- SYSTEM blocked on POST prepare-render-job (AdminActorResolutionException)
- ADMIN/REVIEWER/CREATOR allowed
- TEST_FALLBACK compatibility input (null → ADMIN)
- GET endpoint is unprivileged read-only

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

---

## Deferred: Task 03

Task 03 — Render Execution Safety + Stub Runner Backend will add:
- POST /admin/turku-video-projects/{id}/run-render-job
- GET /admin/turku-video-projects/{id}/render-execution-safety
- StubTurkuRenderExecutionAdapter
- TurkuRenderExecutionSafetyEnvelope
