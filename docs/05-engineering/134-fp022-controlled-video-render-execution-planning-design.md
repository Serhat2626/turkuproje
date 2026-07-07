# FP-022 — Controlled Video Render Execution Planning Design

## 1. Baseline and Status

| Field | Value |
|-------|-------|
| **Feature** | FP-022 — Controlled Video Render Execution Planning |
| **Prerequisite** | FP-001 → FP-021 CLOSED |
| **Latest Commit** | 414bca5 |
| **Backend Tests** | 3255 passed, 0 failures |
| **Frontend Tests** | 1230 passed, 0 failures |
| **Combined Tests** | 4485 |
| **Endpoints** | 40 = 23 POST + 17 GET |
| **Migration/schema/config** | None |

### Current Product State

| Capability | Status | Source |
|-----------|--------|--------|
| Türkü Story-to-Video internal workflow | ✅ Delivered | FP-019 |
| Controlled provider-draft generation | ✅ Delivered | FP-020 |
| Composition plan + render manifest + readiness | ✅ Delivered | FP-021 |
| Real video rendering | ❌ Not yet | FP-022 scope |
| Audio/music/TTS | ❌ Not yet | Future |
| Export/publish/public delivery | ❌ Forbidden | Safety constraint |
| Production auth | ❌ Not yet | Future |

---

## 2. Product Goal

Move the internal Türkü video workflow from **render manifest/readiness planning** to **controlled internal render execution**.

### What FP-022 enables:
- Create an internal render job from an existing render manifest
- Validate render readiness before execution
- Execute or simulate a render job safely (deterministic/stubbed by default)
- Track render attempt, status, and failure codes
- Expose internal render execution status and safety envelope

### What FP-022 does NOT enable:
- ❌ Public export or publish
- ❌ Downloadable video file
- ❌ Public/signed/CDN URL
- ❌ Auto-approval
- ❌ Real FFmpeg/cloud rendering (unless explicitly scoped in a later task)
- ❌ Audio/music/TTS integration
- ❌ Production auth

All render outputs remain **internal metadata only**, subject to admin human review.

---

## 3. Target Workflow

```
Existing Project (RENDER_MANIFEST_PREPARED)
  │
  ├── Render Readiness → validated (NEEDS_REVIEW or BLOCKED)
  │
  ├── POST prepare-render-job
  │     → creates TurkuRenderJob with PREPARED status
  │     → validates manifest completeness
  │     → humanReviewRequired=true, autoApproved=false
  │     → stage → RENDER_JOB_PREPARED
  │
  ├── POST run-render-job
  │     → creates TurkuRenderAttempt
  │     → executes via render adapter (stub by default)
  │     → produces TurkuRenderExecutionResult
  │     → wraps in TurkuRenderExecutionSafetyEnvelope
  │     → stage → RENDER_JOB_RUNNING → RENDER_JOB_COMPLETED_INTERNAL
  │              or → RENDER_JOB_FAILED_SAFE
  │
  ├── GET render-job
  │     → returns current render job status, attempt history
  │     → safe internal metadata only
  │
  ├── GET render-execution-safety
  │     → returns safety envelope, review gates, failure codes
  │     → no raw/binary/provider/storage data
  │
  └── INTERNAL_RENDER_ARTIFACT_READY (if applicable)
        → opaque internal artifact ref only
        → no file path, no public URL, no download
        → still NOT publish/export ready
```

**Key invariant:** Completing a render job internally does NOT make the project publish/export ready. All render outputs are internal, require human review, and remain non-public.

---

## 4. Domain Concepts

### TurkuRenderJob

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | |
| projectId | UUID | |
| manifestRef | String | Opaque internal ref to render manifest |
| status | TurkuRenderJobStatus | |
| humanReviewRequired | boolean | Always true |
| copyrightReviewRequired | boolean | Always true |
| autoApproved | boolean | Always false |
| createdAt | Instant | |
| updatedAt | Instant | |
| attempts | List\<TurkuRenderAttempt\> | |
| executionPlan | TurkuRenderExecutionPlan | |
| safetyEnvelope | TurkuRenderExecutionSafetyEnvelope | |

### TurkuRenderJobRequest

| Field | Type | Notes |
|-------|------|-------|
| renderMode | String | "DETERMINISTIC_STUB" or "INTERNAL_DRAFT" |
| actorRole | ReviewActorRole | TEST_FALLBACK only; SYSTEM blocked |
| actorReference | String | |

### TurkuRenderJobStatus

| Value | Description |
|-------|------------|
| PREPARED | Job created, not yet started |
| RUNNING | Execution in progress |
| COMPLETED_INTERNAL | Render completed, internal only |
| FAILED_SAFE | Failed safely, no partial output exposed |
| CANCELLED | Cancelled by admin |

Explicitly **forbidden** statuses:
- ~~PUBLISHED~~
- ~~EXPORTED~~
- ~~PUBLICLY_DELIVERED~~
- ~~AUTO_APPROVED~~
- ~~DOWNLOAD_READY~~

### TurkuRenderAttempt

| Field | Type | Notes |
|-------|------|-------|
| attemptId | UUID | |
| attemptNo | int | 1-indexed |
| startedAt | Instant | |
| completedAt | Instant | nullable |
| status | TurkuRenderJobStatus | |
| failureCode | TurkuRenderFailureCode | nullable |
| durationMs | long | |
| rendererAlias | String | e.g. "deterministic-stub" |

### TurkuRenderArtifact

| Field | Type | Notes |
|-------|------|-------|
| artifactId | UUID | |
| artifactRef | String | Opaque internal ref only |
| format | String | e.g. "mp4" |
| durationSeconds | double | |
| resolution | String | e.g. "1920x1080" |
| sizeLabel | String | e.g. "estimated: ~50MB" — NOT actual file size |
| humanReviewRequired | boolean | Always true |
| autoApproved | boolean | Always false |

**Forbidden fields on TurkuRenderArtifact:**
- ~~filePath~~, ~~storageKey~~, ~~publicUrl~~, ~~signedUrl~~, ~~cdnUrl~~
- ~~base64~~, ~~binaryData~~, ~~videoData~~, ~~downloadUrl~~
- ~~exportUrl~~, ~~publishUrl~~

### TurkuRenderExecutionPlan

| Field | Type | Notes |
|-------|------|-------|
| sceneCount | int | |
| totalDurationSeconds | double | |
| targetResolution | String | |
| targetFps | int | |
| targetFormat | String | |
| estimatedRenderTimeSeconds | double | |
| rendererAlias | String | |
| renderMode | String | |

### TurkuRenderExecutionResult

| Field | Type | Notes |
|-------|------|-------|
| success | boolean | |
| durationMs | long | |
| failureCode | TurkuRenderFailureCode | nullable |
| failureMessage | String | Safe internal message |
| artifact | TurkuRenderArtifact | nullable, only on success |
| safeTraceRef | String | Opaque trace reference |

### TurkuRenderFailureCode

| Code | Description |
|------|------------|
| MANIFEST_INCOMPLETE | Render manifest missing required data |
| READINESS_CHECK_FAILED | Pre-render readiness validation failed |
| RENDERER_UNAVAILABLE | Renderer adapter not available |
| RENDERER_TIMEOUT | Render execution timed out |
| RENDERER_INTERNAL_ERROR | Internal renderer error |
| SAFETY_VIOLATION | Safety policy blocked execution |
| COPYRIGHT_REVIEW_PENDING | Copyright review not completed |
| HUMAN_REVIEW_REQUIRED | Human review gate not passed |

### TurkuRenderExecutionSafetyEnvelope

| Field | Type | Notes |
|-------|------|-------|
| humanReviewRequired | boolean | Always true |
| copyrightReviewRequired | boolean | Always true |
| autoApproved | boolean | Always false |
| lyricsIncluded | boolean | Always false |
| copyrightClearanceVerified | boolean | Always false |
| renderMode | String | |
| safetyFlags | List\<String\> | |
| reviewFlags | List\<String\> | NOT_PUBLISH_READY, NOT_EXPORT_READY, etc. |
| failureCodes | List\<TurkuRenderFailureCode\> | |
| blockedReasons | List\<String\> | |

---

## 5. Workflow Stages Proposal

### New Stages (5)

| Stage | Description |
|-------|------------|
| RENDER_JOB_PREPARED | Render job created, awaiting execution |
| RENDER_JOB_RUNNING | Render execution in progress |
| RENDER_JOB_COMPLETED_INTERNAL | Render completed, internal only, NOT publish ready |
| RENDER_JOB_FAILED_SAFE | Render failed safely, no partial output exposed |
| INTERNAL_RENDER_ARTIFACT_READY | Internal artifact metadata available, no download/export |

### Explicitly Forbidden Stages (never to be added)

| Forbidden Stage | Reason |
|----------------|--------|
| PUBLISHED | No public delivery |
| EXPORTED | No export |
| PUBLICLY_DELIVERED | No CDN/public path |
| AUTO_APPROVED | No auto-approval |
| DOWNLOAD_READY | No downloadable file |
| RENDER_COMPLETE_PUBLIC | No public render |

### Updated Stage Machine (19 stages after FP-022)

```
DRAFT_CREATED → STORY_CONTEXT_PREPARED → SCENARIO_PREPARED → STORYBOARD_PREPARED
  → SCENE_ASSET_PLAN_PREPARED → IMAGE_BATCH_PREPARED → IMAGE_BATCH_COMPLETED
  → COMPOSITION_PLAN_PREPARED → RENDER_MANIFEST_PREPARED
  → RENDER_JOB_PREPARED → RENDER_JOB_RUNNING
  → RENDER_JOB_COMPLETED_INTERNAL or RENDER_JOB_FAILED_SAFE
  → INTERNAL_RENDER_ARTIFACT_READY
  + IMAGE_BATCH_PARTIAL, INTERNAL_PREVIEW_READY, NEEDS_REVISION, BLOCKED, ARCHIVED
```

---

## 6. API Design Proposal (Future Endpoints Only)

> **Note:** These are design proposals only. No endpoint changes in Task 01.

### New Endpoints (4 proposed)

| Method | Path | Description |
|--------|------|------------|
| POST | `/admin/turku-video-projects/{id}/prepare-render-job` | Create render job from manifest |
| POST | `/admin/turku-video-projects/{id}/run-render-job` | Execute or simulate render |
| GET | `/admin/turku-video-projects/{id}/render-job` | Read current render job status |
| GET | `/admin/turku-video-projects/{id}/render-execution-safety` | Read render execution safety |

### Expected Inventory After FP-022

| Scope | POST | GET | Total |
|-------|:----:|:---:|:-----:|
| Before FP-022 | 23 | 17 | 40 |
| FP-022 adds | +2 | +2 | +4 |
| **After FP-022** | **25** | **19** | **44** |

### Endpoint Safety Rules

- All POST endpoints require AdminActorResolver (SYSTEM blocked)
- All GET endpoints are read-only, unprivileged
- No PUT/DELETE endpoints
- No publish/export/download endpoints
- No Authorization header handling
- Response bodies contain safe metadata only

---

## 7. Render Execution Strategy

### Default: Deterministic Stubbed Render

- No real FFmpeg or cloud render integration in initial tasks
- Stub renderer produces deterministic metadata based on manifest
- Artifact ref is opaque internal identifier (not a file path)
- Execution result includes safe timing/status metadata
- No binary output in any DTO

### Future Renderer Adapter Pattern

```
TurkuRenderExecutionAdapter (interface)
  ├── StubTurkuRenderExecutionAdapter (default)
  │     → deterministic metadata, no real render
  └── InternalDraftTurkuRenderAdapter (future)
        → real render via FFmpeg or cloud, gated by config
```

### Forbidden in Render DTOs

- No binary video data (`byte[]`, `InputStream`, `base64`)
- No downloadable/exportable artifact reference
- No public/signed/CDN URL
- No storage key or file path
- No raw renderer output payload

---

## 8. Render Artifact Policy

| Rule | Enforcement |
|------|------------|
| Artifact ref is opaque internal identifier only | DTO field naming + test scan |
| No file path or storage key | Forbidden field assertion |
| No public URL | Forbidden field assertion |
| No signed URL | Forbidden field assertion |
| No base64/binary data | Forbidden field assertion |
| No frontend-playable video file | No `<video>` element, no player |
| No download button | No `btn-download` |
| Artifact metadata only (format, duration, resolution, size label) | DTO structure |
| Human review required before any future use | Safety envelope |

---

## 9. Audio/Music/Narration Policy

| Rule | Status |
|------|--------|
| No copyrighted audio/music download or copy | Enforced |
| No lyrics body reproduction | Enforced |
| Narration remains original summary/segment metadata | Enforced |
| TTS integration deferred | Not in FP-022 scope |
| Music selection/licensing deferred | Not in FP-022 scope |
| Audio track assembly deferred | Not in FP-022 scope |

---

## 10. Admin Review Gates

| Gate | Value | Notes |
|------|-------|-------|
| humanReviewRequired | true | Always, on every render job and artifact |
| copyrightReviewRequired | true | Always |
| autoApproved | false | Always — no auto-approval path |
| lyricsIncluded | false | Always — no lyrics in render |
| copyrightClearanceVerified | false | Always — manual process |
| RENDER_JOB_COMPLETED_INTERNAL ≠ publish ready | By design | Completing render does NOT unlock publish/export |
| SYSTEM role blocked on POST | AdminActorResolver | SYSTEM cannot create/run render jobs |
| actorRole | TEST_FALLBACK only | No production auth roles |

---

## 11. Frontend/Admin UI Design

### Render Job Panel
- Render job status badge (PREPARED / RUNNING / COMPLETED_INTERNAL / FAILED_SAFE)
- Attempt history list
- Execution timing
- Renderer alias label

### Render Status Timeline
- Visual stage progression through render-related stages
- Clear labeling: "Dahili render — yayın/dışa aktarım yok"

### Safe Internal Artifact Metadata
- Format, duration, resolution, size label
- Opaque artifact ref (NOT a link, NOT clickable)
- No preview, no player, no thumbnail from rendered output

### Failure/Safety Panel
- Failure code + message
- Safety envelope details
- Review flags: NOT_PUBLISH_READY, NOT_EXPORT_READY, INTERNAL_ONLY
- Blocked reasons if applicable

### Forbidden UI Elements
- ❌ No `<video>` element
- ❌ No video player component
- ❌ No download button
- ❌ No export button
- ❌ No publish button
- ❌ No public URL display
- ❌ No signed URL display
- ❌ No CDN URL display
- ❌ No dangerouslySetInnerHTML / innerHTML

---

## 12. Safety Baseline

| Rule | Status |
|------|:------:|
| No publish/export endpoints | ✅ Maintained |
| No public/signed/CDN delivery | ✅ Maintained |
| No auto-approval | ✅ Maintained |
| No generic status update | ✅ Maintained |
| No provider/storage/raw/base64/secret exposure | ✅ Maintained |
| No unsafe HTML | ✅ Maintained |
| No token/JWT/session/principal/authorizationHeader | ✅ Maintained |
| No copyrighted lyrics/audio/video reproduction | ✅ Maintained |
| No downloadable file | ✅ Maintained |
| No video player element | ✅ Maintained |
| humanReviewRequired=true always | ✅ Maintained |
| copyrightReviewRequired=true always | ✅ Maintained |
| SYSTEM role blocked on POST | ✅ Maintained |

---

## 13. Proposed Dense FP-022 Task Plan

### Task 01 — Design (this doc)
- Documentation-only
- No code/endpoint/test changes
- Establishes domain model, API design, safety rules

### Task 02 — Render Job Backend
- Add TurkuRenderJob, TurkuRenderJobStatus, TurkuRenderAttempt, TurkuRenderArtifact domain models
- Add TurkuRenderExecutionPlan, TurkuRenderExecutionResult, TurkuRenderFailureCode
- Add TurkuRenderExecutionSafetyEnvelope
- Add TurkuRenderJobService
- Add POST prepare-render-job endpoint
- Add GET render-job endpoint
- Add 5 new workflow stages
- Tests: endpoint inventory, DTO safety, stage machine, happy path, safety invariants
- Expected endpoints: 42 = 24 POST + 18 GET

### Task 03 — Render Execution Safety + Stub Runner Backend
- Add StubTurkuRenderExecutionAdapter
- Add POST run-render-job endpoint
- Add GET render-execution-safety endpoint
- Add TurkuRenderExecutionSafetyResponse
- Tests: E2E stub render flow, safety envelope, failure modes, SYSTEM blocking
- Expected endpoints: 44 = 25 POST + 19 GET

### Task 04 — Admin UI Render Job / Execution Safety Panel
- Add render job types + API methods to frontend
- Add Render Job Panel, Status Timeline, Artifact Metadata, Failure/Safety Panel
- Add stage labels/actions for render-related stages
- Tests: component structure, safety, backward compat
- Expected endpoints: 44 = 25 POST + 19 GET (no new endpoints)

### Task 05 — E2E Render Execution Safety Regression + Closure
- Full E2E regression: bootstrap → ... → render job → execution → safety
- Backend + frontend regression tests
- Safety baseline verification
- Closure doc: 135-fp022-final-closure-operational-baseline.md
- FP-022 status: CLOSED

---

## 14. Acceptance Criteria for FP-022 Closure

| Criteria | Verification |
|----------|-------------|
| Render job can be created from manifest | E2E test |
| Render job can be executed (stub) | E2E test |
| Render execution produces safe internal metadata | DTO scan |
| No binary/downloadable/public output | Forbidden field + method scan |
| humanReviewRequired=true on all outputs | Assertion |
| autoApproved=false on all outputs | Assertion |
| SYSTEM blocked on all POST | Actor boundary test |
| No publish/export/download endpoints | Endpoint inventory |
| Frontend renders render job/safety panels | Source scan |
| No video player/download/export UI | Source scan |
| Endpoint inventory: 44 = 25 POST + 19 GET | Count assertion |
| Backend + frontend tests pass with 0 failures | CI |
| Closure doc created | File existence |

---

## 15. Task 02 Readiness Checklist

| Item | Status |
|------|:------:|
| Design doc (this file) accepted | ⬜ |
| Domain model defined | ✅ (Section 4) |
| API design defined | ✅ (Section 6) |
| Safety rules defined | ✅ (Sections 8, 10, 12) |
| Forbidden fields defined | ✅ (Sections 4, 7, 8) |
| Workflow stages defined | ✅ (Section 5) |
| Render execution strategy defined | ✅ (Section 7) |
| Frontend design outlined | ✅ (Section 11) |
| Task plan approved | ⬜ |

**Ready for Task 02** once this design doc is accepted.
