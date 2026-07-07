# FP-019 Task 03 — Story/Scenario/Storyboard Orchestration Backend

## Status

| Field | Value |
|-------|-------|
| Task | FP-019 Task 03 |
| Status | **COMPLETED** |
| Type | Backend production + tests + docs |
| Frontend changes | None |
| Migration/schema/config | None |

---

## Endpoints Added

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/turku-video-projects/{id}/prepare-story` | Prepare story/cultural context package |
| POST | `/admin/turku-video-projects/{id}/prepare-scenario` | Prepare 6-beat scenario plan |
| POST | `/admin/turku-video-projects/{id}/prepare-storyboard` | Prepare scene-by-scene storyboard |

### Endpoint Inventory

| Controller | POST | GET | Total |
|-----------|:----:|:---:|:-----:|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| AdminWorkflowObservabilityController | 0 | 2 | 2 |
| TurkuVideoProjectController | **4** | 2 | **6** |
| **Total** | **13** | **14** | **27** |

---

## Workflow Transition Summary

```
DRAFT_CREATED ──→ STORY_CONTEXT_PREPARED ──→ SCENARIO_PREPARED ──→ STORYBOARD_PREPARED
   (bootstrap)       (prepare-story)          (prepare-scenario)    (prepare-storyboard)
```

- Each transition is strictly enforced.
- Wrong-stage requests return safe `INVALID_STAGE_TRANSITION` error.
- Missing prerequisite (e.g. no story before scenario) returns `MISSING_PREREQUISITE`.

---

## Story Package Summary

- **Content**: turkuName, regionContext, originSummary, emotionalTheme, culturalSignificance, narrativeAngle, storySummary
- **Safety**: needsHumanReview=true, copyrightReviewRequired=true
- **Flags**: LYRICS_NOT_INCLUDED, COPYRIGHT_REVIEW_REQUIRED, SOURCE_REVIEW_REQUIRED (when LOW/UNKNOWN), CULTURAL_CONTEXT_REVIEW_REQUIRED
- **Wording**: Cautious — "uzman incelemesi önerilir"

## Scenario Package Summary

- **Content**: title, logline, narrationTone, targetDurationHintSeconds, sceneCountHint, scenarioBeats
- **Beats**: 6-beat structure (Hook, Giriş, Hikâye, Duygusal Zirve, Kapanış, CTA)
- **Flags**: LYRICS_NOT_INCLUDED, COPYRIGHT_REVIEW_REQUIRED, SOURCE_REVIEW_REQUIRED

## Storyboard Package Summary

- **Content**: scenes (sceneNo, sceneTitle, visualDescription, narrationSummary, mood, estimatedDurationSeconds, cameraDirection, transitionHint, musicNote)
- **Flags**: LYRICS_NOT_INCLUDED, COPYRIGHT_REVIEW_REQUIRED, CULTURAL_CONTEXT_REVIEW_REQUIRED
- **Music**: Text labels only ("telif kontrol gerekli"), no audio files

---

## AI / Provider Usage

- **Generation mode**: Deterministic internal stubs (no external AI provider in Task 03)
- **Rationale**: Safer for foundation layer; provider-ready seams available for future tasks
- **No raw provider payload exposure**
- **No provider secrets/log dumps**
- Provider integration can be added in Task 04 or a future task when a safe adapter is established

---

## Source Confidence / Human Review

| Condition | sourceConfidence | needsHumanReview |
|-----------|:---------------:|:----------------:|
| No source notes | UNKNOWN | true |
| Source notes present | LOW | true |
| All stages | preserved | preserved |

- copyrightReviewRequired: always true
- LOW/UNKNOWN confidence triggers SOURCE_REVIEW_REQUIRED flag

---

## Actor Boundary

All 3 new POST endpoints route through `AdminActorResolver` → `AdminActorBoundaryPolicy.enforce()`:

| Role | Planning Allowed? | Mode |
|------|:-----------------:|:----:|
| ADMIN | ✅ | TEST_FALLBACK |
| REVIEWER | ✅ | TEST_FALLBACK |
| CREATOR | ✅ | TEST_FALLBACK |
| SYSTEM | ❌ | Blocked |

actorRole is TEST_FALLBACK compatibility input only — NOT production auth.

---

## Safety Baseline

- ✅ No publish/export/public delivery
- ✅ No public/signed/CDN URLs
- ✅ No lyrics body stored or exposed
- ✅ No copyrighted audio/video
- ✅ No provider/storage/raw/base64/secret exposure
- ✅ No token/JWT/session/principal exposure
- ✅ No unsafe HTML (validated at service layer)
- ✅ No auto-approval
- ✅ No image generation yet
- ✅ No video rendering yet
- ✅ No migration/schema/config changes

---

## Deferred

- **Task 04**: Scene Asset Plan + Image Generation Batch Integration
