# FP-020 Task 03 — Story / Scenario / Storyboard Provider Draft Integration

## Status

| Field | Value |
|-------|-------|
| Task | FP-020 Task 03 |
| Status | **COMPLETED** |
| Type | Backend + controller |
| Frontend changes | None |
| Endpoint count | **34 = 20 POST + 14 GET** |
| Migration/schema/config | None |

---

## Endpoints Added (4 POST)

| # | Method | Path | Purpose |
|---|--------|------|---------|
| 1 | POST | `/admin/turku-video-projects/{id}/generate-story` | Provider-draft story |
| 2 | POST | `/admin/turku-video-projects/{id}/generate-scenario` | Provider-draft scenario |
| 3 | POST | `/admin/turku-video-projects/{id}/generate-storyboard` | Provider-draft storyboard |
| 4 | POST | `/admin/turku-video-projects/{id}/generate-planning-bundle` | Full bundle (story→scenario→storyboard) |

### Full Endpoint Inventory (34)

| Controller | POST | GET | Total |
|-----------|:----:|:---:|:-----:|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| AdminWorkflowObservabilityController | 0 | 2 | 2 |
| TurkuVideoProjectController | 11 | 2 | 13 |
| **Total** | **20** | **14** | **34** |

---

## Service/Controller/DTO Summary

### New Services/Components

| Type | Purpose |
|------|---------|
| `TurkuProviderDraftPlanningService` | Orchestrates generate-* paths with gateway+envelope+policy+sanitizer |
| `StubTurkuGenerationProviderGateway` | Deterministic stub implementation of provider gateway |

### New DTOs

| Type | Purpose |
|------|---------|
| `TurkuProviderGenerationRequest` | Safe input for generate-* endpoints |
| `TurkuProviderPlanningGenerationResponse` | Safe response for single-stage generation |
| `TurkuPlanningBundleGenerationResponse` | Safe response for planning bundle |

---

## Provider Mode Behavior

| Mode | Story/Scenario/Storyboard | Bundle |
|------|---------------------------|--------|
| `DETERMINISTIC_STUB` | Generate through stub gateway | Full sequence |
| `PROVIDER_DRAFT` | Generate through real/stub gateway | Full sequence |
| `PROVIDER_DISABLED` | Return safe disabled response | Return safe disabled |
| `PROVIDER_FAILED_SAFE` | Return safe failure response | Return safe failure |
| `HUMAN_REVIEW_REQUIRED` | Block — return review pending | Block — return review pending |

---

## Safety Envelope Usage

Every generate-* call:
1. Creates `TurkuGenerationSafetyEnvelope.createDefault()`
2. Calls `TurkuProviderSafetyPolicy.enforcePreGenerationPolicy()`
3. Sanitizes input via `TurkuProviderSafetySanitizer`
4. Calls `TurkuGenerationProviderGateway.generateAi()`
5. Calls `TurkuProviderSafetySanitizer.sanitizeEnvelope()`
6. Calls `TurkuProviderSafetyPolicy.enforcePostGenerationPolicy()`
7. Returns safe response

---

## Sanitizer/Policy Behavior

| Check | When | Action |
|-------|------|--------|
| Prompt injection in input | Before generation | Exception (400) |
| Forbidden pattern in input | Before generation | Exception (400) |
| autoApproved=true | Pre + post | Exception (403) |
| humanReviewRequired=false | Pre + post | Exception (403) |
| lyricsIncluded=true | Post | Exception (403) |
| copyrightClearanceVerified=true | Post | Exception (403) |

---

## Workflow/Stage Behavior

| Endpoint | Required Stage | Result Stage |
|----------|---------------|-------------|
| generate-story | DRAFT_CREATED | STORY_CONTEXT_PREPARED |
| generate-scenario | STORY_CONTEXT_PREPARED | SCENARIO_PREPARED |
| generate-storyboard | SCENARIO_PREPARED | STORYBOARD_PREPARED |
| generate-planning-bundle | DRAFT_CREATED | STORYBOARD_PREPARED |

Stages are the same as FP-019 prepare-* — fully compatible.

---

## Actor Boundary Summary

All 4 new POST endpoints:
- Route through `resolvePlanningActor()` → `AdminActorResolver`
- Enforce `AdminActorBoundaryPolicy`
- actorRole = TEST_FALLBACK compatibility input only
- SYSTEM blocked
- ADMIN/REVIEWER/CREATOR allowed

---

## Deferred

- No image generation in Task 03
- No video/render/export/publish
- Real provider adapter wiring deferred to actual provider integration
- Image batch provider integration → Task 04

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
