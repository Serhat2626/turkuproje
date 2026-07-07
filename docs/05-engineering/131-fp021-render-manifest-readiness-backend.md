# FP-021 Task 03 — Render Manifest + Readiness Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-021 Task 03 |
| **Type** | Backend |
| **Status** | COMPLETED |
| **Endpoints** | 39 = 23 POST + 16 GET |
| **Migration/schema/config** | None |

---

## Endpoints Added (2)

| Method | Path | Description |
|--------|------|------------|
| POST | `/admin/turku-video-projects/{id}/prepare-render-manifest` | Generate render manifest from composition plan |
| GET | `/admin/turku-video-projects/{id}/render-readiness` | Evaluate internal render readiness |

### TurkuVideoProjectController Inventory: 14 POST + 4 GET = 18

---

## DTO/Service/Controller Summary

### New Domain Types
| Type | Purpose |
|------|---------|
| TurkuRenderManifest | Complete render instruction set |
| TurkuRenderManifestScene | Single scene in render manifest |
| TurkuRenderReadinessReport | Internal readiness assessment |
| TurkuRenderReadinessIssue | Single readiness issue |
| TurkuRenderReadinessStatus | Enum: READY / NEEDS_REVIEW / BLOCKED |
| TurkuRenderSafetyState | Safety context for render operations |
| TurkuRenderManifestRequest | Request DTO (empty — derived from composition) |
| TurkuRenderManifestResponse | Safe response DTO |

### Service: TurkuRenderManifestService
- Prepares render manifest from composition plan
- Evaluates render readiness (read-only)
- Calculates readiness score (0-100)
- Identifies blocking/warning issues

---

## Workflow Transition Summary

```
COMPOSITION_PLAN_PREPARED → RENDER_MANIFEST_PREPARED
```

Earlier stages block safely with `IllegalStateException`.

---

## Render Manifest Structure

```
TurkuRenderManifest
├── manifestId (UUID)
├── projectId, compositionId (UUID refs)
├── scenes: List<TurkuRenderManifestScene>
│   ├── sceneNo, sceneTitle
│   ├── imageRef (safe internal "internal-ref:xxx")
│   ├── narrationRef (safe "narration-ref:xxx")
│   ├── musicRef (null placeholder)
│   ├── durationSeconds
│   ├── transitionIn, transitionOut
│   └── cameraDirection
├── targetResolution ("1920x1080")
├── targetFps (30), targetFormat ("mp4")
├── totalDurationSeconds
├── humanReviewRequired=true
├── copyrightReviewRequired=true
├── autoApproved=false
└── reviewFlags (INTERNAL_READINESS_ONLY, etc.)
```

---

## Render Readiness Behavior

### Readiness Report
- **compositionReady**: composition plan exists
- **manifestReady**: render manifest prepared
- **allScenesHaveImages**: no missing image refs
- **allNarrationPresent**: all narration segments have text
- **timingPlanComplete**: no timing conflicts
- **copyrightReviewDone**: always false (manual process)
- **readinessScore**: 0-100 weighted percentage

### Status
- **READY**: all prerequisites met (internal render only)
- **NEEDS_REVIEW**: non-blocking issues present
- **BLOCKED**: blocking prerequisites missing

### Issues
| Code | Severity | Meaning |
|------|----------|---------|
| MISSING_COMPOSITION | BLOCKING | No composition plan |
| MISSING_MANIFEST | BLOCKING | No render manifest |
| MISSING_IMAGES | BLOCKING | Scene images missing |
| MISSING_NARRATION | WARNING | Narration text incomplete |
| TIMING_CONFLICT | WARNING | Timing plan has conflicts |
| COPYRIGHT_PENDING | WARNING | Copyright review incomplete |

---

## Internal-Readiness vs Publish-Readiness

| Concept | Scope | FP-021 |
|---------|-------|--------|
| Internal render readiness | All data present for internal preview | ✅ In scope |
| Publish readiness | Safe for public delivery | ❌ NOT in scope |
| Export readiness | Safe for file download | ❌ NOT in scope |

Review flags always include `NOT_PUBLISH_READY` and `NOT_EXPORT_READY`.

---

## No-Render / No-Export Policy

- No actual video rendering (no FFmpeg, no video file)
- No downloadable file produced
- No binary video/audio data in responses
- Manifest is a planning artifact only
- All refs are safe internal opaque UUIDs

---

## Actor Boundary Summary

- POST endpoint: AdminActorResolver enforces boundary
- GET endpoint: read-only, no actor resolution needed
- SYSTEM role blocked from POST requests
- actorRole remains TEST_FALLBACK compatibility input only

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

## Deferred: Task 04 — Admin UI Composition Timeline / Readiness Preview

Next task adds:
- GET /admin/turku-video-projects/{id}/composition-preview
- Frontend composition timeline panel
- Readiness dashboard
- Narration segment list
