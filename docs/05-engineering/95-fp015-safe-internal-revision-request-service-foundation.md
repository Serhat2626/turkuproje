# FP-015 Task 02 — Safe Internal Revision Request Service Foundation

## 1. Task Status

| Field | Value |
|-------|-------|
| Task | Task 02 — Safe Internal Revision Request Service Foundation |
| Status | **COMPLETED** |
| Type | Backend service + tests only |
| Controller/endpoint | No |
| Frontend | No |
| Migration | No |

## 2. Scope

Backend-only service foundation for internal revision requests. Wraps existing ReviewApplicationService with additional actor/state validation and idempotent note append behavior.

## 3. Changed Files

### Backend Production — Modified

| File | Changes |
|------|---------|
| `ReviewActionType.java` | +1 value: REQUEST_INTERNAL_REVISION |
| `ReviewAuditEventType.java` | +1 value: INTERNAL_REVISION_REQUESTED |
| `ReviewTransitionPolicy.java` | +1 transition: APPROVED_FOR_INTERNAL_USE + REQUEST_INTERNAL_REVISION → CHANGES_REQUESTED; +1 actor check; Map.of → Map.ofEntries for 9 entries |
| `ReviewAuditEventFactory.java` | +1 case: REQUEST_INTERNAL_REVISION → INTERNAL_REVISION_REQUESTED |
| `ReviewApplicationService.java` | +1 method: requestInternalRevision(); +1 switch case in safeLabelFor() |

### Backend Production — New

| File | Description |
|------|-------------|
| `InternalRevisionRequest.java` | Safe immutable request DTO with builder |
| `InternalRevisionRequestResult.java` | Safe immutable result DTO with builder |
| `InternalRevisionRequestException.java` | Safe exception with error code |
| `InternalRevisionRequestService.java` | Service wrapping ReviewApplicationService |

### Backend Tests — New

| File | Tests |
|------|-------|
| `InternalRevisionRequestServiceTest.java` | 50+ tests across 9 categories |

## 4. Service Design

### InternalRevisionRequestService

Spring `@Service` that wraps `ReviewApplicationService` for the narrow `REQUEST_INTERNAL_REVISION` action.

**Dependencies:** `ReviewApplicationService` only — no direct repository or domain service injection.

**Method:** `requestRevision(InternalRevisionRequest request)` → `InternalRevisionRequestResult`

### Processing Flow

```
InternalRevisionRequest
  ↓ validate actor role (REVIEWER/ADMIN only)
  ↓ get current review state via ReviewApplicationService
  ↓ route by current state:
    ├─ APPROVED_FOR_INTERNAL_USE → delegate to ReviewApplicationService.requestInternalRevision()
    ├─ CHANGES_REQUESTED → delegate to ReviewApplicationService.addReviewNote() (idempotent)
    └─ all other states → throw InternalRevisionRequestException
  ↓
InternalRevisionRequestResult
```

## 5. Request/Result Objects

### InternalRevisionRequest

| Field | Type | Required | Sanitization |
|-------|------|----------|-------------|
| targetType | ReviewTargetType | ✅ | — |
| targetId | UUID | ✅ | — |
| actorRole | ReviewActorRole | ✅ | — |
| actorReference | String | ✅ | — |
| safeReasonCode | String | ❌ | ReviewTextSafety.sanitizeReasonCode() |
| safeMessage | String | ✅ | ReviewTextSafety.sanitize() |

### InternalRevisionRequestResult

| Field | Type | Description |
|-------|------|-------------|
| previousState | ReviewState | State before action |
| currentState | ReviewState | State after action |
| actionApplied | String | REQUEST_INTERNAL_REVISION or NOTE_APPENDED |
| safeReasonCode | String | Sanitized reason |
| safeMessage | String | Sanitized message |
| updatedAt | LocalDateTime | Timestamp |
| appendedNote | boolean | True if note appended (idempotent case) |
| auditRecorded | boolean | True if audit trail entry created |

## 6. Allowed Actors

| Actor Role | Allowed? | Behavior |
|-----------|----------|----------|
| REVIEWER | ✅ Yes | Can request revision |
| ADMIN | ✅ Yes | Can request revision |
| SYSTEM | ❌ No | Rejected with SYSTEM_NOT_ALLOWED |
| CREATOR | ❌ No | Rejected with CREATOR_NOT_ALLOWED |

## 7. Allowed States & Transition Rules

| Source State | Action | Target State | Behavior |
|-------------|--------|-------------|----------|
| APPROVED_FOR_INTERNAL_USE | REQUEST_INTERNAL_REVISION | CHANGES_REQUESTED | State transition via domain model |
| CHANGES_REQUESTED | — | CHANGES_REQUESTED | Note append only (idempotent) |
| DRAFT | — | — | Rejected: STATE_NOT_ALLOWED |
| AI_GENERATED | — | — | Rejected: STATE_NOT_ALLOWED |
| NEEDS_REVIEW | — | — | Rejected: STATE_NOT_ALLOWED |
| IN_REVIEW | — | — | Rejected: STATE_NOT_ALLOWED |
| REJECTED | — | — | Rejected: STATE_NOT_ALLOWED |
| Missing record | — | — | Rejected: REVIEW_NOT_FOUND |

## 8. Idempotency

When the current state is already CHANGES_REQUESTED:
- No duplicate state transition occurs
- A safe note is appended via `ReviewApplicationService.addReviewNote()`
- Result indicates `actionApplied=NOTE_APPENDED`, `appendedNote=true`
- Audit is recorded via existing note audit mechanism

## 9. Audit Behaviour

- State transition (APPROVED → CHANGES_REQUESTED): audit event INTERNAL_REVISION_REQUESTED
- Note append (CHANGES_REQUESTED): audit event REVIEW_NOTE_ADDED
- Both paths use existing ReviewApplicationService audit infrastructure
- No custom audit bypass

## 10. Safety

| Safety Item | Status |
|------------|--------|
| Publish/export | ❌ Not possible |
| Approve | ❌ Not possible |
| Return to draft | ❌ Not possible |
| Generate/regenerate | ❌ Not possible |
| Provider/storage calls | ❌ Not possible |
| Public/signed/CDN URL | ❌ Not possible |
| Provider URL exposure | ❌ No field exists |
| Storage key exposure | ❌ No field exists |
| Base64/bytes/raw payload | ❌ No field exists |
| Secret/API key/token | ❌ No field exists |
| Stack trace display | ❌ Safe toString only |
| Direct repository mutation | ❌ Only via ReviewApplicationService |

## 11. Test Coverage

| Category | Tests | Coverage |
|----------|-------|----------|
| 1. Happy Path | 4 | APPROVED→CHANGES_REQUESTED, audit, reason code, timestamp |
| 2. Idempotent Note Append | 3 | CHANGES_REQUESTED note, no duplicate transition, ADMIN note |
| 3. Forbidden States | 6 | DRAFT, AI_GENERATED, NEEDS_REVIEW, IN_REVIEW, REJECTED, missing |
| 4. Actor Validation | 5 | REVIEWER ok, ADMIN ok, SYSTEM rejected, CREATOR rejected, null |
| 5. Safety Regression | 7 | Forbidden fields (3 DTOs), forbidden methods, toString safety (3) |
| 6. No Write Bypass | 3 | No repo injection, no domain service, no write on forbidden |
| 7. Target Type Coverage | 5 | CONTENT_PROJECT, SCENARIO, STORYBOARD, PACKAGE, IMAGE_ASSET |
| 8. Domain Model Integration | 12 | Enum values, transition policy, audit factory, actor checks, forbidden values |
| 9. Request Validation | 7 | Null fields, blank fields, sanitization |
| **Total** | **52** | |

## 12. Deferred Items

| Item | Status |
|------|--------|
| Controller/endpoint | Task 03 |
| Frontend UI | Task 04 |
| Approved inventory UI | Task 04 |
| End-to-end regression | Task 05 |
| CSS polish | Future |
| Production auth integration | Future |

## 13. Recommendation for Task 03

**Task 03 — Internal Revision Request Admin API**

- Add `POST /api/admin/reviews/{targetType}/{targetId}/request-internal-revision`
- Safe request/response DTO mapping
- Controller exception handling
- Endpoint count: 16 → 17 (9 POST + 8 GET)
- Controller tests
- No migration
