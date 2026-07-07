# FP-020 Task 04 — Controlled Image Batch Provider Integration

## Status

| Field | Value |
|-------|-------|
| Task | FP-020 Task 04 |
| Status | **COMPLETED** |
| Type | Backend + controller |
| Frontend changes | None |
| Endpoint count | **35 = 21 POST + 14 GET** |
| Migration/schema/config | None |

---

## Endpoint Added (1 POST)

| # | Method | Path | Purpose |
|---|--------|------|---------|
| 1 | POST | `/admin/turku-video-projects/{id}/run-image-batch` | Provider-draft image batch execution |

### Full Endpoint Inventory (35)

| Controller | POST | GET | Total |
|-----------|:----:|:---:|:-----:|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| AdminWorkflowObservabilityController | 0 | 2 | 2 |
| TurkuVideoProjectController | 12 | 2 | 14 |
| **Total** | **21** | **14** | **35** |

---

## Service/Controller/DTO Summary

### New Services/Components

| Type | Purpose |
|------|---------|
| `TurkuProviderImageBatchService` | Provider-backed image batch execution with gateway+envelope+policy+sanitizer |

### New DTOs

| Type | Purpose |
|------|---------|
| `TurkuImageBatchRunRequest` | Safe input for run-image-batch endpoint |
| `TurkuImageBatchRunResponse` | Safe response with per-scene statuses + opaque refs |
| `TurkuImageBatchRunSceneStatus` | Per-scene status within batch run response |

---

## Provider Mode Behavior

| Mode | Behavior |
|------|----------|
| `DETERMINISTIC_STUB` | Run through stub gateway — opaque internal refs |
| `PROVIDER_DRAFT` | Run through real/stub gateway — opaque internal refs |
| `PROVIDER_DISABLED` | Return safe disabled response (BLOCKED) |
| `PROVIDER_FAILED_SAFE` | Return safe failure response (BLOCKED) |
| `HUMAN_REVIEW_REQUIRED` | Block run safely (BLOCKED) |

---

## Image Gateway/Envelope/Sanitizer Usage

Every scene in the batch:
1. Creates `TurkuGenerationSafetyEnvelope.createDefault()` per scene
2. Calls `TurkuProviderSafetyPolicy.enforcePreGenerationPolicy()`
3. Builds `TurkuImageGenerationRequest` with safe scene fields
4. Calls `TurkuGenerationProviderGateway.generateImage()`
5. Calls `TurkuProviderSafetySanitizer.sanitizeEnvelope()`
6. Calls `TurkuProviderSafetyPolicy.enforcePostGenerationPolicy()`
7. Aggregates per-scene result with opaque internal ref

---

## Opaque Internal Ref Policy

- Scene image results use `internal-ref:` prefixed opaque references
- No base64 image data
- No public/signed/CDN URLs
- No storage keys (S3, GCS, etc.)
- No raw provider payloads
- All refs are meaningless outside the system

---

## Workflow/Stage Behavior

| Endpoint | Required Stage | Result Stage |
|----------|---------------|-------------|
| run-image-batch | IMAGE_BATCH_PREPARED | IMAGE_BATCH_COMPLETED (all success) |
| run-image-batch | IMAGE_BATCH_PREPARED | IMAGE_BATCH_PARTIAL (some success) |

Existing FP-019 endpoints preserved:
- `prepare-image-batch` — still creates batch at IMAGE_BATCH_PREPARED
- `complete-image-batch` — still completes batch at IMAGE_BATCH_COMPLETED

---

## Actor Boundary Summary

Run-image-batch endpoint:
- Routes through `resolvePlanningActor()` → `AdminActorResolver`
- Enforces `AdminActorBoundaryPolicy`
- actorRole = TEST_FALLBACK compatibility input only
- SYSTEM blocked
- ADMIN/REVIEWER/CREATOR allowed

---

## Deferred

- No video rendering/assembly
- No export/publish/public delivery
- No final completed public video state
- Real image provider adapter wiring → future task
- **Task 05 → Admin UI Provider Mode / Generation Safety Visibility**

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
