# FP-027 Task 02 — Backend Queue/Retry/Expiration Model + Service + Integration

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-027 Task 02 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |

---

## Added Backend Classes (15)

| Class | Kind |
|-------|------|
| TurkuInternalRenderQueueState | enum (10 states, 4 terminal) |
| TurkuInternalRenderQueueAction | enum (9 actions) |
| TurkuInternalRenderQueueReasonCode | enum (20 codes) |
| TurkuInternalRenderQueueItemRef | opaque ref |
| TurkuInternalRenderQueueTransitionResult | immutable result |
| TurkuInternalRenderQueueTransitionValidator | 17-transition matrix |
| TurkuInternalRenderQueueSummary | safe summary DTO |
| TurkuInternalRenderQueueActionResult | action result wrapper |
| TurkuInternalRenderQueueService | stateless service |
| TurkuInternalRenderRetryPolicy | bounded retry (0-5 attempts, 30-600s) |
| TurkuInternalRenderRetryReasonCode | enum (7 codes) |
| TurkuInternalRenderRetrySnapshot | retry snapshot |
| TurkuInternalRenderExpirationPolicy | bounded expiration |
| TurkuInternalRenderExpirationReasonCode | enum (5 codes) |
| TurkuInternalRenderExpirationSnapshot | expiration snapshot |

---

## Queue States (10) / Actions (9)

**States**: NOT_QUEUED, QUEUE_READY, QUEUED_INTERNAL, DISPATCHING_INTERNAL, IN_PROGRESS_INTERNAL, RETRY_WAIT_INTERNAL, SUCCEEDED_INTERNAL, FAILED_INTERNAL, CANCELLED_INTERNAL, EXPIRED_INTERNAL

**Terminal**: SUCCEEDED, FAILED, CANCELLED, EXPIRED (no resurrection)

**Actions**: MARK_QUEUE_READY, ENQUEUE_INTERNAL, DISPATCH_INTERNAL, MARK_IN_PROGRESS, MARK_RETRY_WAIT, MARK_SUCCEEDED, MARK_FAILED, CANCEL_INTERNAL, EXPIRE_INTERNAL

**Forbidden**: UPDATE_STATUS, SET_STATUS, PATCH_STATUS, PUBLISH, EXPORT, DOWNLOAD, AUTO_APPROVE, PUBLIC_DELIVERY

---

## Transition Matrix (17 allowed)

Happy path (8) + Cancel from 6 states + Expire from 3 states = 17

---

## Retry / Expiration Bounds

- Max attempts: clamped 0–5
- Retry delay: clamped 30–600s
- Queue duration: clamped 60–7200s
- Retry wait duration: clamped 60–3600s

---

## FP-025/026 Dispatch Integration

Dispatch requires ALL:
- FP-025 readiness = READY_INTERNAL_ONLY
- FP-025 safety chain = all safe
- FP-026 review = APPROVED_FOR_INTERNAL_NEXT_STEP

---

## Service Behavior

- createQueueItem: initial NOT_QUEUED
- applyAction: explicit actions through validator
- isDispatchReady: FP-025 + FP-026 integration
- No generic setStatus/updateStatus
- No publish/export/download/autoApprove

---

## Safety Scan

- 15 classes × 33 forbidden fields = **0 violations** ✅
- publicDeliveryAvailable = false (always)
- autoApproved = false (always)
- internalOnly = true (always)
