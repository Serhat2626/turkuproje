# FP-019 Task 02 — Türkü Video Project Bootstrap Backend

## Status

| Field | Value |
|-------|-------|
| Task | FP-019 Task 02 |
| Status | **COMPLETED** |
| Type | Backend production + tests + docs |
| Frontend changes | None |
| Migration/schema/config | None |

---

## Endpoints Added

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/turku-video-projects/bootstrap` | Create new project from türkü name |
| GET | `/admin/turku-video-projects/{id}` | Get project details (safe fields) |
| GET | `/admin/turku-video-projects/{id}/progress` | Get workflow progress |

### Endpoint Inventory

| Controller | POST | GET | Total |
|-----------|:----:|:---:|:-----:|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| AdminWorkflowObservabilityController | 0 | 2 | 2 |
| **TurkuVideoProjectController** | **1** | **2** | **3** |
| **Total** | **10** | **14** | **24** |

---

## DTOs / Domain Objects

| Class | Purpose |
|-------|---------|
| `TurkuVideoProject` | In-memory domain object, safe fields only |
| `TurkuVideoWorkflowStage` | 12-value enum, no forbidden states |
| `TurkuSourceConfidence` | UNKNOWN / LOW / MEDIUM / HIGH |
| `TurkuVideoBootstrapRequest` | Request DTO: türkü name + optional context |
| `TurkuVideoBootstrapResponse` | Response DTO: id, name, stage, message |
| `TurkuVideoProjectResponse` | Detail response: all safe project fields |
| `TurkuVideoProgressResponse` | Progress: stage, completed stages, next action |
| `TurkuVideoProjectRepository` | ConcurrentHashMap-based in-memory store |
| `TurkuVideoProjectBootstrapService` | Validation, sanitization, project creation |
| `TurkuVideoProjectController` | REST controller, 3 endpoints |
| `TurkuVideoProjectException` | Safe exception with error codes |

---

## Initial Workflow Stage Behavior

- All projects start in `DRAFT_CREATED`.
- Stage advancement requires explicit admin action (future tasks).
- No auto-progression. No auto-approval.

## Source Confidence & Human Review

- `sourceConfidence` defaults to `UNKNOWN` (no source notes) or `LOW` (source notes present but unverified).
- `needsHumanReview` defaults to `true` for all new projects.
- `copyrightReviewRequired` defaults to `true` for all new projects.

## Copyright Review

- Every türkü project is flagged `copyrightReviewRequired=true`.
- No lyrics body stored or exposed.
- No copyrighted audio/video references.
- Admin must review copyright implications before advancing workflow.

---

## Actor Boundary Behavior

- POST bootstrap routes through `AdminActorResolver` → `AdminActorBoundaryPolicy.enforce()`.
- `actorRole` in request is `TEST_FALLBACK` compatibility input only — NOT production auth.
- `TURKU_PROJECT_BOOTSTRAP` action category added to `AdminActorBoundaryPolicy`.

| Actor Role | Bootstrap Allowed? |
|:----------:|:-----------------:|
| ADMIN | ✅ |
| REVIEWER | ✅ |
| CREATOR | ✅ |
| SYSTEM | ❌ |

---

## Validation Rules

| Field | Rule |
|-------|------|
| turkuName | Required, 2-200 chars, no HTML tags, no script, no credential patterns |
| region | Optional, max 100 chars |
| sourceNotes | Optional, max 500 chars |
| mood | Optional, max 200 chars |
| targetDurationMinutes | Optional, 1-30 range |
| narrationStyle | Optional, max 100 chars |
| visualStyle | Optional, max 100 chars |

---

## Safety Baseline

- ✅ No publish/export/public delivery
- ✅ No public/signed/CDN URLs
- ✅ No lyrics/copyrighted media storage
- ✅ No provider/storage/raw/base64/secret exposure
- ✅ No token/JWT/session/principal/authorizationHeader
- ✅ No unsafe HTML (stripped at service layer)
- ✅ No auto-approval
- ✅ No migration/schema/config changes
- ✅ No AI generation yet
- ✅ No video/image generation yet

---

## Deferred

- **Task 03**: Story/Scenario/Storyboard Orchestration Backend
- AI generation integration
- Image/video generation
- Frontend integration
