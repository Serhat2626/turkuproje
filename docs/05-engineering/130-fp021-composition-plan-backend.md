# FP-021 Task 02 — Composition Plan Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-021 Task 02 |
| **Type** | Backend |
| **Status** | COMPLETED |
| **Endpoints** | 37 = 22 POST + 15 GET |
| **Migration/schema/config** | None |

---

## Endpoint Added (1)

| Method | Path | Description |
|--------|------|------------|
| POST | `/admin/turku-video-projects/{id}/prepare-composition-plan` | Assemble composition plan from storyboard + images + narration |

### TurkuVideoProjectController Inventory: 13 POST + 3 GET = 16

---

## DTO/Service/Controller Summary

### New Domain Types
| Type | Purpose |
|------|---------|
| TurkuVideoCompositionPlan | Top-level composition aggregate |
| TurkuVideoCompositionScene | Single scene in composition timeline |
| TurkuNarrationSegment | Original narration summary (NOT lyrics) |
| TurkuSceneTimingPlan | Duration breakdown for all scenes |
| TurkuTransitionPlan | Scene-to-scene transition directives |
| TurkuCompositionPlanRequest | Request DTO (empty — derived from project) |
| TurkuCompositionPlanResponse | Safe response DTO |

### Service
- **TurkuVideoCompositionService**: Assembles composition plan from storyboard + scene images + narration + timing. Validates stage, builds scenes/narration/timing/transitions, advances project stage.

### Controller
- POST endpoint uses AdminActorResolver boundary
- SYSTEM role blocked
- Returns safe DTO only

---

## Workflow Transition Summary

```
IMAGE_BATCH_COMPLETED → COMPOSITION_PLAN_PREPARED
IMAGE_BATCH_PARTIAL → COMPOSITION_PLAN_PREPARED (with PARTIAL review flag)
```

Earlier stages (DRAFT_CREATED, STORYBOARD_PREPARED, etc.) block safely.

---

## Composition Plan Structure

```
TurkuVideoCompositionPlan
├── compositionId (UUID)
├── projectId (UUID)
├── totalDurationSeconds
├── scenes: List<TurkuVideoCompositionScene>
│   ├── sceneNo, sceneTitle, visualDescription
│   ├── narrationText (original, NOT lyrics)
│   ├── mood, durationSeconds
│   ├── imageRef (safe internal ref "internal-ref:xxx")
│   ├── transitionIn, transitionOut
│   └── cameraDirection
├── narrationSegments: List<TurkuNarrationSegment>
│   ├── segmentNo, sceneNo
│   ├── narrationText (original summary)
│   ├── durationSeconds, voiceStyle, language
│   └── ttsReady (always false initially)
├── timingPlan: TurkuSceneTimingPlan
│   ├── totalDuration, targetDuration, delta
│   ├── sceneDurations map
│   └── hasTimingConflict
├── transitionPlan: TurkuTransitionPlan
│   ├── transitions list
│   └── defaultTransition
├── humanReviewRequired=true
├── copyrightReviewRequired=true
├── autoApproved=false
└── reviewFlags
```

---

## Narration / No-Lyrics Policy

- Narration segments contain **original summary text** derived from storyboard narrationSummary
- NOT copyrighted lyrics
- Language: "tr"
- Voice style: "documentary"
- TTS readiness: false (deferred)

---

## Safe Internal Refs Policy

- Image refs: `internal-ref:<safeInternalRef>` format
- No public URLs, signed URLs, CDN URLs
- No base64, binary data, storage keys
- No downloadable files
- Opaque UUID-based references only

---

## Actor Boundary Summary

- AdminActorResolver enforces boundary
- SYSTEM role blocked from client requests
- ADMIN/REVIEWER/CREATOR allowed via TEST_FALLBACK
- actorRole is compatibility input only, NOT production auth

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

## Deferred: Task 03 — Render Manifest + Readiness Backend

Next task adds:
- TurkuRenderManifest
- TurkuRenderReadinessReport
- POST prepare-render-manifest
- GET render-readiness
