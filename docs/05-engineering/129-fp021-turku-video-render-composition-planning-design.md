# FP-021 — Türkü Video Render / Composition Planning Design

## 1. Baseline and Status

| Field | Value |
|-------|-------|
| **Preceding features** | FP-001 → FP-020 CLOSED |
| **Latest commit** | f2e0e3f |
| **Backend tests** | 3127 |
| **Frontend tests** | 1087 |
| **Combined tests** | 4214 |
| **Endpoints** | 36 = 21 POST + 15 GET |
| **Migration/schema/config** | None |

### Current Product State

FP-019 delivered internal Türkü Story-to-Video workflow:
```
türkü name → project → story → scenario → storyboard
  → scene assets → image batch → admin internal preview
```

FP-020 added controlled provider-draft path:
```
generate story/scenario/storyboard/bundle
  → run image batch → generation safety visibility
```

**Not yet implemented:**
- Final video rendering/composition
- Audio/music handling
- Export/publish/public delivery
- Production auth

---

## 2. Product Goal

Move from the existing internal image/story workflow toward an **internal video composition package**.

The composition plan assembles all existing workflow outputs — storyboard scenes, generated images, narration scripts, timing/duration directives, and transition metadata — into a structured render manifest that describes what a final video *would* contain, without actually rendering it.

**Key constraints:**
- Still admin-reviewed and not public
- No actual video file produced in FP-021
- No export/publish/download
- humanReviewRequired=true, autoApproved=false always
- Composition plan = planning artifact, not final deliverable

---

## 3. Target Workflow

```
Existing project (IMAGE_BATCH_COMPLETED or later)
  → Composition plan preparation
    ├── Storyboard scenes (ordered, with visual descriptions)
    ├── Scene asset/image batch refs (safe internal refs only)
    ├── Narration script summaries (original, not copyrighted lyrics)
    ├── Timing/duration plan (per-scene seconds, total duration)
    ├── Transition plan (scene-to-scene transitions)
    └── Music/audio placeholder refs (no actual files)
  → Render manifest preparation
    ├── Ordered scene list with all refs
    ├── Narration segments with timing
    ├── Transition directives
    ├── Target resolution/format metadata
    └── Safety/review flags
  → Render readiness check
    ├── All scenes have images? ✅/❌
    ├── All narration segments present? ✅/❌
    ├── Timing plan complete? ✅/❌
    ├── Copyright review done? ✅/❌
    ├── Human review required? (always ✅)
    └── Readiness score / blocking issues
  → Internal composition preview metadata
    ├── Scene timeline visualization data
    ├── Duration breakdown
    ├── Narration text preview
    └── Readiness flags
```

---

## 4. Domain Concepts

### TurkuVideoCompositionPlan

Top-level aggregate for the video composition.

| Field | Type | Description |
|-------|------|------------|
| compositionId | UUID | Unique ID |
| projectId | UUID | Parent project ref |
| totalDurationSeconds | int | Target total duration |
| sceneCount | int | Number of composition scenes |
| scenes | List\<TurkuVideoCompositionScene\> | Ordered scene list |
| narrationSegments | List\<TurkuNarrationSegment\> | Narration plan |
| timingPlan | TurkuSceneTimingPlan | Timing breakdown |
| transitionPlan | TurkuTransitionPlan | Scene transitions |
| status | CompositionPlanStatus | DRAFT / PREPARED / REVIEWED |
| humanReviewRequired | boolean | Always true |
| copyrightReviewRequired | boolean | Always true |
| reviewFlags | List\<String\> | Safety/review flags |
| createdAt | Instant | Creation timestamp |

### TurkuVideoCompositionScene

One scene in the composition timeline.

| Field | Type | Description |
|-------|------|------------|
| sceneNo | int | Scene number (1-indexed) |
| sceneTitle | String | Short title |
| visualDescription | String | What the scene shows |
| narrationText | String | Original narration summary (not lyrics) |
| mood | String | Scene mood/emotion |
| durationSeconds | double | Scene duration |
| imageRef | String | Safe internal ref to generated image |
| transitionIn | String | Transition type entering scene |
| transitionOut | String | Transition type leaving scene |
| cameraDirection | String | Camera movement directive |
| musicRef | String | Safe internal ref (placeholder) |

### TurkuNarrationSegment

One segment of narration for the video.

| Field | Type | Description |
|-------|------|------------|
| segmentNo | int | Segment number |
| sceneNo | int | Associated scene |
| narrationText | String | Original summary text (NOT lyrics) |
| durationSeconds | double | Segment duration |
| voiceStyle | String | Voice style directive |
| language | String | Language code (tr) |
| ttsReady | boolean | Whether TTS could process this (future) |

### TurkuSceneTimingPlan

Timing breakdown for all scenes.

| Field | Type | Description |
|-------|------|------------|
| totalDurationSeconds | double | Sum of all scene durations |
| targetDurationSeconds | int | Target total (from project config) |
| durationDelta | double | Over/under target |
| sceneDurations | Map\<Integer, Double\> | Scene# → duration |
| hasTimingConflict | boolean | Any scene too long/short |

### TurkuTransitionPlan

Transition directives between scenes.

| Field | Type | Description |
|-------|------|------------|
| transitions | List\<TransitionDirective\> | Ordered transitions |
| defaultTransition | String | Default type (e.g., "CROSS_FADE") |

**TransitionDirective:**
| Field | Type |
|-------|------|
| fromScene | int |
| toScene | int |
| type | String (CROSS_FADE, CUT, DISSOLVE, FADE_BLACK) |
| durationMs | int |

### TurkuRenderManifest

Complete render instruction set.

| Field | Type | Description |
|-------|------|------------|
| manifestId | UUID | Unique ID |
| projectId | UUID | Parent project |
| compositionId | UUID | Parent composition |
| scenes | List\<RenderSceneEntry\> | Ordered render entries |
| targetResolution | String | e.g., "1920x1080" |
| targetFps | int | e.g., 30 |
| targetFormat | String | e.g., "mp4" |
| totalDurationSeconds | double | Expected total |
| humanReviewRequired | boolean | Always true |
| autoApproved | boolean | Always false |
| status | RenderManifestStatus | DRAFT / PREPARED / VALIDATED |

**RenderSceneEntry:**
| Field | Type |
|-------|------|
| sceneNo | int |
| imageRef | String (safe internal) |
| narrationRef | String (safe internal) |
| musicRef | String (safe internal, placeholder) |
| durationSeconds | double |
| transitionIn | String |
| transitionOut | String |
| cameraDirection | String |

### TurkuRenderReadinessReport

Readiness assessment for render.

| Field | Type | Description |
|-------|------|------------|
| projectId | UUID | Project ref |
| compositionReady | boolean | Composition plan exists |
| manifestReady | boolean | Manifest prepared |
| allScenesHaveImages | boolean | No missing image refs |
| allNarrationPresent | boolean | All segments have text |
| timingPlanComplete | boolean | No timing conflicts |
| copyrightReviewDone | boolean | Always false (no auto) |
| humanReviewRequired | boolean | Always true |
| autoApproved | boolean | Always false |
| readyForRender | boolean | All checks pass |
| blockingIssues | List\<String\> | What's blocking |
| readinessScore | int | 0-100 percentage |

### TurkuRenderSafetyState

Safety context for render operations.

| Field | Type | Description |
|-------|------|------------|
| projectId | UUID | Project ref |
| renderAllowed | boolean | Whether render can proceed |
| humanReviewRequired | boolean | Always true |
| autoApproved | boolean | Always false |
| copyrightClearanceVerified | boolean | Always false (manual) |
| lyricsIncluded | boolean | Always false |
| audioSourceSafe | boolean | No copyrighted audio |
| safeFailureCode | String | Reason if blocked |
| safeWarnings | List\<String\> | Warning messages |
| reviewFlags | List\<String\> | Active flags |

---

## 5. Workflow Stages Proposal

New stages to add to `TurkuVideoWorkflowStage`:

| Stage | Description | After |
|-------|------------|-------|
| COMPOSITION_PLAN_PREPARED | Composition plan assembled | IMAGE_BATCH_COMPLETED |
| RENDER_MANIFEST_PREPARED | Render manifest generated | COMPOSITION_PLAN_PREPARED |
| RENDER_READINESS_READY | All readiness checks pass | RENDER_MANIFEST_PREPARED |
| INTERNAL_RENDER_PREVIEW_READY | Preview metadata available | RENDER_READINESS_READY |
| RENDER_BLOCKED | Render blocked (missing data/review) | Any of above |

### Explicitly Forbidden Stages

These stages MUST NOT be added in FP-021 or any future FP without explicit safety review:

| Forbidden Stage | Reason |
|----------------|--------|
| PUBLISHED | No public delivery |
| EXPORTED | No file export |
| PUBLICLY_DELIVERED | No CDN/public URL |
| AUTO_APPROVED | No auto-approval ever |
| DOWNLOAD_READY | No downloadable files |
| PUBLIC_READY | No public access |

---

## 6. API Design Proposal (Future Endpoints Only)

> **⚠️ These are design-only proposals. No endpoint changes in Task 01.**

### Proposed new endpoints (4):

| # | Method | Path | Purpose |
|---|--------|------|---------|
| 1 | POST | `/admin/turku-video-projects/{id}/prepare-composition-plan` | Assemble composition plan from storyboard + images + narration |
| 2 | POST | `/admin/turku-video-projects/{id}/prepare-render-manifest` | Generate render manifest from composition |
| 3 | GET | `/admin/turku-video-projects/{id}/render-readiness` | Check render readiness |
| 4 | GET | `/admin/turku-video-projects/{id}/composition-preview` | Get composition timeline preview metadata |

**Projected endpoint inventory after FP-021:**
- 40 = 23 POST + 17 GET (if all 4 added)

### Endpoint contracts (design):

**POST prepare-composition-plan:**
- Input: project ID (composition derived from existing data)
- Output: `TurkuVideoCompositionPlan` (safe fields only)
- Requires: IMAGE_BATCH_COMPLETED stage
- Routes through AdminActorResolver
- Sets stage → COMPOSITION_PLAN_PREPARED

**POST prepare-render-manifest:**
- Input: project ID
- Output: `TurkuRenderManifest` (safe fields only)
- Requires: COMPOSITION_PLAN_PREPARED stage
- Routes through AdminActorResolver
- Sets stage → RENDER_MANIFEST_PREPARED

**GET render-readiness:**
- Input: project ID
- Output: `TurkuRenderReadinessReport`
- Read-only, no state change
- Safe fields only

**GET composition-preview:**
- Input: project ID
- Output: composition timeline metadata
- Read-only, no state change
- Safe internal refs only

---

## 7. Render/Composition Strategy

### What FP-021 delivers:
- **Manifest-only approach**: The render manifest describes what a video would contain
- **No actual rendering**: No FFmpeg, no video file generation
- **Planning artifact**: The composition plan is a structured planning document
- **Safe opaque refs**: All image/audio/narration references are safe internal IDs

### What FP-021 does NOT deliver:
| Excluded | Reason |
|----------|--------|
| Real video rendering | Deferred to future FP |
| Downloadable video file | No export/download |
| Binary video data in responses | No binary data exposure |
| Public/signed/CDN URL | No public delivery |
| FFmpeg integration | Deferred |
| Video streaming | Deferred |

---

## 8. Audio/Music/Narration Policy

### Narration:
- Narration segments contain **original summary text** written by the system
- Narration text is NOT copyrighted lyrics
- Real TTS (text-to-speech) generation deferred to future FP
- `ttsReady` flag indicates whether segment is TTS-eligible (always false initially)

### Music:
- Music refs are **safe placeholders** only
- No actual music files downloaded, stored, or served
- No copyrighted recording reproduction
- Future music handling requires explicit copyright clearance workflow

### Lyrics:
- **No lyrics dumped into narration segments**
- `lyricsIncluded=false` always
- Narration text describes the türkü's story, does not reproduce lyrics
- Any lyrics usage requires future explicit copyright workflow

### Copyright:
- `copyrightReviewRequired=true` always
- `copyrightClearanceVerified=false` always (manual process)
- No automated copyright clearance

---

## 9. Provider/Storage Safety

| Rule | Enforcement |
|------|------------|
| No raw provider payloads | Sanitizer strips before response |
| No storage keys in responses | Internal refs only |
| No base64/binary data | Text metadata only |
| No public URLs | Internal opaque refs |
| No signed URLs | No URL generation |
| No CDN URLs | No CDN integration |
| Internal refs only | UUID-based opaque references |
| No provider endpoint exposure | Gateway abstraction |

All image/audio/narration references in composition plans and render manifests are **safe opaque UUIDs** that cannot be used to access external resources directly.

---

## 10. Admin Review Gates

| Gate | Value | Enforcement |
|------|-------|------------|
| humanReviewRequired | true | Hardcoded, cannot be disabled |
| autoApproved | false | Hardcoded, cannot be enabled |
| Render readiness ≠ publish readiness | Yes | Separate concepts by design |
| SYSTEM cannot auto-approve | Yes | AdminActorResolver blocks SYSTEM |
| actorRole | TEST_FALLBACK only | No production auth |
| copyrightReviewRequired | true | Always required |

**Render readiness** means:
- All scenes have image refs
- All narration segments present
- Timing plan complete
- No blocking issues

**Render readiness does NOT mean:**
- Ready to publish
- Ready to export
- Ready for public access
- Copyright cleared
- Human review completed

---

## 11. Frontend/Admin Preview Design

### Composition Timeline Panel

Visual timeline showing:
- Scene sequence with thumbnails (safe internal refs → placeholder display)
- Duration per scene (seconds)
- Total duration vs target
- Timing conflicts highlighted

### Scene Detail Cards

Per-scene display:
- Scene number and title
- Visual description
- Narration text summary
- Duration
- Transition in/out
- Camera direction
- Image ref status (✅ present / ❌ missing)
- Music ref status (placeholder)

### Narration Segment List

Ordered list:
- Segment number → scene association
- Narration text preview
- Duration
- Voice style
- TTS readiness (always ❌ for now)

### Readiness Dashboard

Status panel:
- Overall readiness score (0-100%)
- Individual checks with ✅/❌
- Blocking issues list
- Review flags
- Safety warnings

### Explicitly absent:
- ❌ No export button
- ❌ No download button
- ❌ No publish button
- ❌ No public URL display
- ❌ No video player
- ❌ No audio player

---

## 12. Safety Baseline

All existing safety rules from FP-019/FP-020 remain in full force:

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No generic status update | ✅ |
| No provider/storage/raw/base64/secret exposure | ✅ |
| No unsafe HTML (dangerouslySetInnerHTML/innerHTML) | ✅ |
| No token/JWT/session/principal/authorizationHeader | ✅ |
| No copyrighted lyrics/audio/video reproduction | ✅ |
| humanReviewRequired=true always | ✅ |
| autoApproved=false always | ✅ |
| lyricsIncluded=false always | ✅ |
| copyrightReviewRequired=true always | ✅ |
| No PUT/DELETE endpoints | ✅ |
| No real video rendering in FP-021 | ✅ |
| No downloadable files | ✅ |
| No binary video/audio data in responses | ✅ |

---

## 13. Proposed FP-021 Task Plan

| Task | Title | Type | Scope |
|------|-------|------|-------|
| **01** | Design doc | Documentation | This document |
| **02** | Composition plan backend | Backend | TurkuVideoCompositionPlan, TurkuVideoCompositionScene, TurkuNarrationSegment, TurkuSceneTimingPlan, TurkuTransitionPlan, service, 1 POST endpoint |
| **03** | Render manifest + readiness backend | Backend | TurkuRenderManifest, TurkuRenderReadinessReport, TurkuRenderSafetyState, service, 1 POST + 1 GET endpoint |
| **04** | Admin UI composition timeline / readiness preview | Backend + Frontend | 1 GET endpoint, composition timeline panel, readiness dashboard, narration list |
| **05** | E2E render-planning safety regression + closure | Tests + Docs | Full regression, closure doc |

### Task 02 scope detail:
- Add TurkuVideoCompositionPlan, TurkuVideoCompositionScene, TurkuNarrationSegment, TurkuSceneTimingPlan, TurkuTransitionPlan domain types
- Add TurkuVideoCompositionService
- Add POST /admin/turku-video-projects/{id}/prepare-composition-plan
- Add COMPOSITION_PLAN_PREPARED stage
- Tests: composition plan preparation, stage transition, safety invariants, DTO safety
- Endpoint count: 37 = 22 POST + 15 GET

### Task 03 scope detail:
- Add TurkuRenderManifest, TurkuRenderReadinessReport, TurkuRenderSafetyState types
- Add TurkuRenderManifestService
- Add POST /admin/turku-video-projects/{id}/prepare-render-manifest
- Add GET /admin/turku-video-projects/{id}/render-readiness
- Add RENDER_MANIFEST_PREPARED, RENDER_READINESS_READY stages
- Endpoint count: 39 = 23 POST + 16 GET

### Task 04 scope detail:
- Add GET /admin/turku-video-projects/{id}/composition-preview
- Add frontend types for composition/render
- Add composition timeline panel
- Add readiness dashboard
- Add narration segment list
- Endpoint count: 40 = 23 POST + 17 GET

### Task 05 scope detail:
- Full E2E render-planning regression
- Safety scan (no export/publish/download, no binary, no lyrics)
- Closure doc
- FP-021 status: CLOSED

---

## 14. Acceptance Criteria for FP-021 Closure

| # | Criterion |
|---|-----------|
| 1 | Composition plan can be prepared from IMAGE_BATCH_COMPLETED project |
| 2 | Render manifest can be prepared from composition plan |
| 3 | Render readiness report accurately reflects missing data |
| 4 | Admin UI shows composition timeline with scene/narration/timing |
| 5 | Admin UI shows readiness dashboard with blocking issues |
| 6 | No export/publish/download controls in UI |
| 7 | No binary video/audio data in any response |
| 8 | No lyrics reproduction |
| 9 | humanReviewRequired=true, autoApproved=false always |
| 10 | All new DTOs pass forbidden-field safety scan |
| 11 | Endpoint inventory matches expected count |
| 12 | All existing FP-019/FP-020 tests still pass |
| 13 | E2E regression passes |

---

## 15. Task 02 Readiness Checklist

| # | Item | Status |
|---|------|:------:|
| 1 | FP-021 design doc accepted | ⬜ |
| 2 | Domain types defined in design | ✅ |
| 3 | API contract defined in design | ✅ |
| 4 | Stage transitions defined | ✅ |
| 5 | Safety baseline documented | ✅ |
| 6 | No blocking dependencies | ✅ |
| 7 | In-memory repository pattern established | ✅ |
| 8 | Controller pattern established (FP-019/020) | ✅ |
| 9 | Test pattern established | ✅ |

**Task 02 is ready to begin once this design doc is accepted.**
