# FP-020 Task 05 — Admin UI Provider Mode / Generation Safety Visibility

## Status

| Field | Value |
|-------|-------|
| Task | FP-020 Task 05 |
| Status | **COMPLETED** |
| Type | Backend + Frontend |
| Endpoint count | **36 = 21 POST + 15 GET** |
| Migration/schema/config | None |

---

## Endpoint Added (1 GET)

| # | Method | Path | Purpose |
|---|--------|------|---------|
| 1 | GET | `/admin/turku-video-projects/{id}/generation-safety` | Safe read-only generation safety status |

### Full Endpoint Inventory (36)

| Controller | POST | GET | Total |
|-----------|:----:|:---:|:-----:|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| AdminWorkflowObservabilityController | 0 | 2 | 2 |
| TurkuVideoProjectController | 12 | 3 | 15 |
| **Total** | **21** | **15** | **36** |

---

## Backend Safety Read Model Summary

### New DTO

| Type | Purpose |
|------|---------|
| `TurkuGenerationSafetyResponse` | Safe read-only view of provider mode, generation availability, flags |

### Response Fields

| Field | Description |
|-------|------------|
| providerMode | Current provider mode (DETERMINISTIC_STUB, PROVIDER_DRAFT, etc.) |
| currentStage | Current workflow stage |
| generationAvailable | Whether generation can proceed |
| blockedReasonSummary | Turkish explanation if blocked |
| humanReviewRequired | Always true |
| autoApproved | Always false |
| copyrightReviewRequired | Always true |
| lyricsIncluded | Always false |
| canGenerate* | Per-action availability based on stage + mode |
| safeFailureCode | Mode name if blocked |
| lastTraceRef | Safe opaque trace reference |
| reviewFlags | Safety review flags |
| safeWarnings | Safe warning messages |

---

## Frontend Types/API/UI Summary

### Types Added (FP-020 block)

| Type | Purpose |
|------|---------|
| `TurkuProviderMode` | Provider mode union (5 values) |
| `TurkuProviderFailureCode` | Failure code union (7 values) |
| `TurkuProviderTraceReference` | Safe trace reference |
| `TurkuGenerationSafetyResponse` | Safety read model |
| `TurkuProviderPlanningGenerationResponse` | Single-stage generation response |
| `TurkuPlanningBundleGenerationResponse` | Bundle generation response |
| `TurkuImageBatchRunRequest` | Image batch run input |
| `TurkuImageBatchRunSceneStatus` | Per-scene status |
| `TurkuImageBatchRunResponse` | Image batch run response |
| `TurkuProviderGenerationRequest` | Generation request input |

### API Client Methods Added (6)

| Method | Endpoint |
|--------|----------|
| `generateTurkuStory` | POST generate-story |
| `generateTurkuScenario` | POST generate-scenario |
| `generateTurkuStoryboard` | POST generate-storyboard |
| `generateTurkuPlanningBundle` | POST generate-planning-bundle |
| `runTurkuImageBatch` | POST run-image-batch |
| `getTurkuGenerationSafety` | GET generation-safety |

### UI Updates

| Section | Description |
|---------|------------|
| Provider Mode Badge | Color-coded badge showing current mode |
| Safety Panel | 6-field detail grid + blocked alert + review flags + warnings + trace ref |
| Generate Actions | 5 buttons (story, scenario, storyboard, bundle, image batch) |
| Blocked Alert | Shows reason when provider is disabled/failed/review-required |

---

## Provider Mode Visibility

| Mode | Badge | Color |
|------|-------|-------|
| DETERMINISTIC_STUB | Deterministik Stub | badge-info |
| PROVIDER_DRAFT | Sağlayıcı Taslak | badge-warning |
| PROVIDER_DISABLED | Sağlayıcı Devre Dışı | badge-error |
| PROVIDER_FAILED_SAFE | Güvenli Hata Modu | badge-error |
| HUMAN_REVIEW_REQUIRED | İnsan İncelemesi Gerekli | badge-warning |

---

## Generation Safety Panel

Shows:
- Provider mode with badge
- Generation availability (✅/❌)
- Human review required (always ✅ Gerekli)
- Auto approved (always ❌ Hayır / Asla)
- Copyright review required (always ✅ Gerekli)
- Lyrics included (always ❌ Dahil Değil)
- Blocked state alert with failure code
- Review flags badges
- Safe warnings
- Safe trace reference

---

## Safety Baseline Preserved

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

---

## Deferred

- **Task 06 → E2E Safety Regression + Final Closure**
