# FP-026 Task 02 — Backend Human Review Gate Model + Transition Validator

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-026 Task 02 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |

---

## Added Backend Classes (7)

| Class | Kind |
|-------|------|
| TurkuInternalRenderReviewState | enum (8 states, 4 terminal) |
| TurkuInternalRenderReviewAction | enum (7 actions) |
| TurkuInternalRenderReviewReasonCode | enum (18 codes) |
| TurkuInternalRenderReviewRef | opaque ref (internal-render-review:{uuid}) |
| TurkuInternalRenderReviewTransitionResult | immutable result |
| TurkuInternalRenderReviewTransitionValidator | 11-transition matrix |
| TurkuInternalRenderReviewSummary | safe summary DTO |

---

## Review Ref Model

- Format: `internal-render-review:{uuid}`
- Parse/validate/factory methods
- Reject null/blank/malformed/wrong prefix
- Safe toString — no path/storage/provider leak

---

## Review States (8)

| State | Terminal | Meaning |
|-------|:--------:|---------|
| REVIEW_NOT_REQUIRED | No | Job does not need review |
| REVIEW_REQUIRED | No | Human review is required |
| READY_FOR_REVIEW | No | Readiness checks pass, awaiting reviewer |
| IN_REVIEW | No | Reviewer is examining |
| APPROVED_FOR_INTERNAL_NEXT_STEP | **Yes** | Internal-only approval — NOT publish |
| REJECTED_BY_HUMAN_REVIEW | **Yes** | Rejected by reviewer |
| BLOCKED_BY_SAFETY | **Yes** | Safety violation |
| EXPIRED_REVIEW | **Yes** | Review window expired |

---

## Review Actions (7)

| Action | Purpose |
|--------|---------|
| REQUIRE_HUMAN_REVIEW | Mark as requiring review |
| MARK_READY_FOR_REVIEW | Mark readiness verified |
| START_HUMAN_REVIEW | Begin examination |
| APPROVE_FOR_INTERNAL_NEXT_STEP | Approve internal-only |
| REJECT_HUMAN_REVIEW | Reject |
| BLOCK_FOR_SAFETY | Block for safety |
| EXPIRE_HUMAN_REVIEW | Expire review window |

Forbidden: AUTO_APPROVE, PUBLISH, EXPORT, DOWNLOAD, UPDATE_STATUS, SET_STATUS, PATCH_STATUS, APPROVE_FOR_PUBLIC_RELEASE

---

## Transition Matrix (11 allowed)

| From | Action | To |
|------|--------|----|
| REVIEW_NOT_REQUIRED | REQUIRE | REVIEW_REQUIRED |
| REVIEW_REQUIRED | MARK_READY | READY_FOR_REVIEW |
| READY_FOR_REVIEW | START | IN_REVIEW |
| IN_REVIEW | APPROVE | APPROVED_FOR_INTERNAL_NEXT_STEP |
| IN_REVIEW | REJECT | REJECTED_BY_HUMAN_REVIEW |
| IN_REVIEW | BLOCK | BLOCKED_BY_SAFETY |
| REVIEW_REQUIRED | BLOCK | BLOCKED_BY_SAFETY |
| READY_FOR_REVIEW | BLOCK | BLOCKED_BY_SAFETY |
| REVIEW_REQUIRED | EXPIRE | EXPIRED_REVIEW |
| READY_FOR_REVIEW | EXPIRE | EXPIRED_REVIEW |
| IN_REVIEW | EXPIRE | EXPIRED_REVIEW |

---

## Terminal State Policy

- APPROVED, REJECTED, BLOCKED, EXPIRED → no transitions out
- All 7 actions denied from terminal states
- Reason code: TERMINAL_STATE
- No resurrection

---

## Auto-Approval / Public Delivery Guarantees

- autoApproved = false (always, in all DTOs)
- publicDeliveryAvailable = false (always)
- internalOnly = true (always)
- notPublishReady = true
- notExportReady = true
- Safety flags: INTERNAL_ONLY, NO_PUBLISH, NO_EXPORT, NO_DOWNLOAD, NO_AUTO_APPROVE

---

## Forbidden Exposure Scan

- 7 classes × 33 forbidden fields = **0 violations** ✅
- No path/storage/URL/command/binary/secret/auth fields
