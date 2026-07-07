# FP-019 Task 04 — Scene Asset Plan + Image Generation Batch Integration

## Status

| Field | Value |
|-------|-------|
| Task | FP-019 Task 04 |
| Status | **COMPLETED** |
| Type | Backend production + tests + docs |
| Frontend changes | None |
| Migration/schema/config | None |

---

## Endpoints Added

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/turku-video-projects/{id}/prepare-scene-assets` | Prepare per-scene visual asset plan |
| POST | `/admin/turku-video-projects/{id}/prepare-image-batch` | Prepare image generation batch |
| POST | `/admin/turku-video-projects/{id}/complete-image-batch` | Complete/partial-mark image batch |

### Endpoint Inventory

| Controller | POST | GET | Total |
|-----------|:----:|:---:|:-----:|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| AdminWorkflowObservabilityController | 0 | 2 | 2 |
| TurkuVideoProjectController | **7** | 2 | **9** |
| **Total** | **16** | **14** | **30** |

---

## Workflow Transition Summary

```
STORYBOARD_PREPARED ──→ SCENE_ASSET_PLAN_PREPARED ──→ IMAGE_BATCH_PREPARED ──→ IMAGE_BATCH_COMPLETED
  (prepare-storyboard)   (prepare-scene-assets)       (prepare-image-batch)    (complete-image-batch)
                                                                                          ↓
                                                                              IMAGE_BATCH_PARTIAL
                                                                              (partial=true)
```

Each transition is strictly enforced. Wrong-stage requests return `INVALID_STAGE_TRANSITION`.

---

## Scene Asset Plan Summary

Per-scene visual asset item generated from storyboard:
- **Fields**: sceneNo, assetPurpose, visualPromptSummary, negativePromptSafetyNotes, styleHint, mood, aspectRatioHint
- **Safety**: needsHumanReview=true, copyrightReviewRequired=true per item
- **Flags**: LYRICS_NOT_INCLUDED, COPYRIGHT_REVIEW_REQUIRED, HUMAN_REVIEW_REQUIRED, PUBLIC_DELIVERY_BLOCKED, SOURCE_REVIEW_REQUIRED (when LOW/UNKNOWN), CULTURAL_CONTEXT_REVIEW_REQUIRED

## Image Batch Summary

- **Status enum**: PREPARED, PARTIAL, COMPLETED, BLOCKED, FAILED
- **Batch contains**: imageRequests (per-scene), imageResults (per-scene with safe opaque internal refs)
- **Stub generation**: Deterministic internal placeholders with `internal-ref:` prefix
- **Completion modes**: full (→IMAGE_BATCH_COMPLETED) or partial (→IMAGE_BATCH_PARTIAL)

---

## Image / Provider / Storage Safety

| Rule | Enforced? |
|------|:---------:|
| No base64 image data | ✅ |
| No public URLs | ✅ |
| No signed URLs | ✅ |
| No CDN URLs | ✅ |
| No storage keys (s3://, gs://) | ✅ |
| No provider request/response dump | ✅ |
| No provider secrets | ✅ |
| No raw binary image data | ✅ |
| No automatic public preview | ✅ |
| Opaque safe internal refs only | ✅ |

## Integration Mode

- **Deterministic internal stubs** — no external image provider in Task 04
- Provider-ready seams available: `TurkuSceneImageRequest` / `TurkuSceneImageResult` abstractions
- Safe adapter integration can be added in future tasks

---

## Actor Boundary

All 3 new POST endpoints route through `AdminActorResolver` → `AdminActorBoundaryPolicy.enforce()`:

| Role | Allowed? | Mode |
|------|:--------:|:----:|
| ADMIN | ✅ | TEST_FALLBACK |
| REVIEWER | ✅ | TEST_FALLBACK |
| CREATOR | ✅ | TEST_FALLBACK |
| SYSTEM | ❌ | Blocked |

actorRole is TEST_FALLBACK compatibility input only — NOT production auth.

---

## Safety Baseline

- ✅ No publish/export/public delivery
- ✅ No public/signed/CDN URLs
- ✅ No base64/binary image data
- ✅ No storage keys
- ✅ No provider raw payloads/secrets
- ✅ No lyrics body
- ✅ No copyrighted audio/video
- ✅ No token/JWT/session/principal exposure
- ✅ No unsafe HTML
- ✅ No auto-approval
- ✅ No video rendering
- ✅ No migration/schema/config changes

---

## Deferred

- **Task 05**: Admin UI Start/Progress/Internal Preview Integration
