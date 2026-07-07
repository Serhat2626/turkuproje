# FP-025 Task 02 — Backend Render Job Lifecycle Model + Transition Validator

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-025 Task 02 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |

---

## Added Backend Classes (8)

| Class | Kind |
|-------|------|
| TurkuInternalRenderJobStatus | enum (8 states) |
| TurkuInternalRenderJobAction | enum (7 actions) |
| TurkuInternalRenderJobReasonCode | enum (14 codes) |
| TurkuInternalRenderJobRef | opaque ref with parse/validate |
| TurkuInternalRenderJobTransitionResult | immutable result |
| TurkuInternalRenderJobTransitionValidator | transition matrix |
| TurkuInternalRenderJobProgress | bounded progress snapshot |
| TurkuInternalRenderJobSummary | safe summary DTO |

---

## Lifecycle States

| State | Terminal |
|-------|:--------:|
| DRAFT | No |
| READY_FOR_INTERNAL_RENDER | No |
| QUEUED_INTERNAL | No |
| RUNNING_INTERNAL | No |
| SUCCEEDED_INTERNAL | No |
| FAILED_INTERNAL | **Yes** |
| CANCELLED_INTERNAL | **Yes** |
| EXPIRED_INTERNAL | **Yes** |

---

## Lifecycle Actions

| Action | Purpose |
|--------|---------|
| VALIDATE_FOR_INTERNAL_RENDER | DRAFT → READY |
| ENQUEUE_INTERNAL_RENDER | READY → QUEUED |
| START_INTERNAL_RENDER | QUEUED → RUNNING |
| COMPLETE_INTERNAL_RENDER | RUNNING → SUCCEEDED |
| FAIL_INTERNAL_RENDER | RUNNING → FAILED |
| CANCEL_INTERNAL_RENDER | Any active → CANCELLED |
| EXPIRE_INTERNAL_RENDER | Any non-terminal → EXPIRED |

No generic UPDATE_STATUS / SET_STATUS / PATCH_STATUS action exists.

---

## Transition Matrix (14 allowed)

| From | Action | To |
|------|--------|----|
| DRAFT | VALIDATE | READY_FOR_INTERNAL_RENDER |
| READY | ENQUEUE | QUEUED_INTERNAL |
| QUEUED | START | RUNNING_INTERNAL |
| RUNNING | COMPLETE | SUCCEEDED_INTERNAL |
| RUNNING | FAIL | FAILED_INTERNAL |
| DRAFT | CANCEL | CANCELLED_INTERNAL |
| READY | CANCEL | CANCELLED_INTERNAL |
| QUEUED | CANCEL | CANCELLED_INTERNAL |
| RUNNING | CANCEL | CANCELLED_INTERNAL |
| DRAFT | EXPIRE | EXPIRED_INTERNAL |
| READY | EXPIRE | EXPIRED_INTERNAL |
| QUEUED | EXPIRE | EXPIRED_INTERNAL |
| RUNNING | EXPIRE | EXPIRED_INTERNAL |
| SUCCEEDED | EXPIRE | EXPIRED_INTERNAL |

---

## Terminal State Policy

- FAILED_INTERNAL, CANCELLED_INTERNAL, EXPIRED_INTERNAL → no transitions out
- All 7 actions denied from terminal states
- Reason code: TERMINAL_STATE
- No resurrection allowed

---

## Safe DTO Inventory

### TurkuInternalRenderJobSummary fields
- jobRef (opaque)
- status
- progressPercent (clamped 0–100)
- currentStageLabel
- safeTraceRef
- humanReviewRequired=true
- copyrightReviewRequired=true
- autoApproved=false
- notPublishReady=true
- notExportReady=true
- safetyFlags

### Forbidden fields absent
8 classes × 33 forbidden fields = **0 violations**

---

## Progress Snapshot Foundation

- Bounded: 0–100 (clamped)
- 7 predefined Turkish stage labels
- humanReviewRequired=true, autoApproved=false
- Safety flags: INTERNAL_ONLY, NO_PUBLISH, NO_EXPORT, NO_DOWNLOAD

---

## Safety Scan Result

- No filePath/absolutePath/workspacePath fields ✅
- No storageKey/publicUrl/signedUrl/cdnUrl fields ✅
- No commandLine/shellCommand/executablePath fields ✅
- No base64/binary/rawPayload fields ✅
- No token/JWT/session/secret/auth fields ✅
- No publish/export/download actions or states ✅
