# FP-015 Task 03 — Internal Revision Request Admin API + Approved Inventory API Hardening

## 1. Task Status

| Field | Value |
|-------|-------|
| Task | Task 03 — Internal Revision Request Admin API |
| Status | **COMPLETED** |
| Type | Backend API + tests only |
| Frontend | No changes |
| Migration | No |

## 2. Scope

Backend API endpoint for internal revision requests + hardening regression tests for existing approved inventory/readiness APIs.

## 3. Changed Files

### Backend Production — Modified

| File | Changes |
|------|---------|
| `AdminReviewController.java` | +1 POST endpoint, +1 service injection, +1 validation method, +1 exception handler; endpoint count 16→17 (9 POST + 8 GET) |

### Backend Production — New

| File | Description |
|------|-------------|
| `AdminInternalRevisionRequest.java` | Safe admin request DTO (body: actorRole, actorReference, safeReasonCode, safeMessage) |
| `AdminInternalRevisionResponse.java` | Safe immutable admin response DTO with builder |

### Backend Tests — New

| File | Tests |
|------|-------|
| `InternalRevisionRequestAdminApiTest.java` | 45+ tests across 10 categories (A–J) |

### Backend Tests — Modified

| File | Changes |
|------|---------|
| `AdminReviewControllerTest.java` | +1 mock injection (InternalRevisionRequestService), updated constructor call |

### Docs — New

| File | Description |
|------|-------------|
| `96-fp015-internal-revision-request-admin-api-and-inventory-hardening.md` | This document |

## 4. New Endpoint

```
POST /api/admin/reviews/{targetType}/{targetId}/request-internal-revision
```

Purpose: Allow REVIEWER/ADMIN to request a safe internal revision.

- Uses path variables for targetType and targetId
- Request body: actorRole, actorReference, safeReasonCode, safeMessage
- Returns safe result with previous/current state, action applied, flags
- Delegates to InternalRevisionRequestService — no direct repository access
- NOT a generic status update

## 5. Endpoint Count Before/After

| Metric | Before | After |
|--------|--------|-------|
| Total endpoints | 16 | 17 |
| POST endpoints | 8 | 9 |
| GET endpoints | 8 | 8 |

## 6. Request DTO

### AdminInternalRevisionRequest

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| actorRole | ReviewActorRole | ✅ | REVIEWER or ADMIN |
| actorReference | String | ✅ | Actor identifier |
| safeReasonCode | String | ❌ | Sanitized reason code |
| safeMessage | String | ✅ | Sanitized revision message |

Note: targetType and targetId come from path variables.

## 7. Response DTO

### AdminInternalRevisionResponse

| Field | Type | Description |
|-------|------|-------------|
| targetType | ReviewTargetType | From path |
| targetId | UUID | From path |
| previousState | ReviewState | State before action |
| currentState | ReviewState | State after action |
| actionApplied | String | REQUEST_INTERNAL_REVISION or NOTE_APPENDED |
| safeReasonCode | String | Sanitized reason |
| safeMessage | String | Sanitized message |
| appendedNote | boolean | True if note appended (idempotent) |
| auditRecorded | boolean | Always true |
| updatedAt | LocalDateTime | Timestamp |

## 8. Actor Behavior

| Actor Role | Allowed? | HTTP Status |
|-----------|----------|-------------|
| REVIEWER | ✅ Yes | 200 OK |
| ADMIN | ✅ Yes | 200 OK |
| SYSTEM | ❌ No | 403 Forbidden |
| CREATOR | ❌ No | 403 Forbidden |

## 9. State Behavior

| Source State | Result | HTTP Status |
|-------------|--------|-------------|
| APPROVED_FOR_INTERNAL_USE | → CHANGES_REQUESTED | 200 OK |
| CHANGES_REQUESTED | → note append (idempotent) | 200 OK |
| DRAFT | Rejected | 409 Conflict |
| AI_GENERATED | Rejected | 409 Conflict |
| NEEDS_REVIEW | Rejected | 409 Conflict |
| IN_REVIEW | Rejected | 409 Conflict |
| REJECTED | Rejected | 409 Conflict |
| Missing record | Not found | 404 Not Found |

## 10. Approved Inventory/Readiness Hardening

| Check | Result |
|-------|--------|
| approved/internal-readiness remains read-only | ✅ Verified (GET only) |
| single target internal-readiness remains read-only | ✅ Verified |
| CHANGES_REQUESTED target returns NOT_APPROVED | ✅ Verified |
| readiness APIs have no forbidden fields | ✅ Verified |
| readiness APIs have no publish/export actions | ✅ Verified |
| internal-readiness endpoints have no POST variant | ✅ Verified |

## 11. Exception/Failure Behavior

| Error Code | HTTP Status | Description |
|-----------|-------------|-------------|
| NULL_REQUEST | 400 Bad Request | Null request |
| NULL_ACTOR_ROLE | 400 Bad Request | Missing actor role |
| SYSTEM_NOT_ALLOWED | 403 Forbidden | SYSTEM actor rejected |
| CREATOR_NOT_ALLOWED | 403 Forbidden | CREATOR actor rejected |
| REVIEW_NOT_FOUND | 404 Not Found | Missing review record |
| STATE_NOT_ALLOWED | 409 Conflict | Forbidden source state |

All error responses: safe message, no stack trace, no secrets, no provider/storage details.

## 12. Safety Boundaries

| Safety Item | Status |
|------------|--------|
| Publish/export | ❌ Not possible |
| Auto-approve | ❌ Not possible |
| Public/signed/CDN URL | ❌ No field exists |
| Provider/storage calls | ❌ Not possible |
| Provider URL exposure | ❌ No field exists |
| Storage key exposure | ❌ No field exists |
| Base64/bytes/raw payload | ❌ No field exists |
| Secret/API key/token | ❌ No field exists |
| Stack trace display | ❌ Safe error only |
| Direct repository mutation | ❌ Only via service |
| Generic status update | ❌ Not possible |
| Generation/regeneration | ❌ Not possible |

## 13. Test Coverage

| Category | Tests | Coverage |
|----------|-------|----------|
| A. Endpoint Happy Path | 6 | REVIEWER/ADMIN success, target, audit, reason, timestamp |
| B. Idempotent Path | 3 | Note append, no auto-approve, no publish/export |
| C. Actor Rejection | 3 | SYSTEM/CREATOR rejected, safe message |
| D. Forbidden State Rejection | 6 | DRAFT, AI_GENERATED, NEEDS_REVIEW, IN_REVIEW, REJECTED, missing |
| E. Request Validation | 5 | Missing/blank actorRole, actorReference, safeMessage |
| F. Safe Response Payload | 5 | Forbidden fields (2 DTOs), toString safety (3) |
| G. Readiness Regression | 5 | Approved list, CHANGES_REQUESTED not ready, safe fields, read-only, no publish actions |
| H. Endpoint Inventory | 5 | Total=17, POST=9, GET=8, new path, no forbidden paths |
| I. No Side Effects | 4 | No repo injection, no forbidden methods, delegates to service, no direct app service |
| J. Migration/Schema | 4 | Controller/service/request/response not entities |
| **Total** | **46** | |

## 14. Migration/Schema Confirmation

- No migrations added
- No schema/entity changes
- No new @Entity or @Table annotations
- No database changes required

## 15. Frontend/Admin Status

- No frontend/admin-web changes
- Frontend tests not run (admin-web not touched)

## 16. Deferred Items

| Item | Status |
|------|--------|
| Frontend UI for revision request | Task 04 |
| Approved inventory admin panel | Task 04 |
| End-to-end regression | Task 05 |
| Production auth integration | Future |
| CSS polish | Future |

## 17. Recommendation for Task 04

**Task 04 — Admin UI Internal Revision Request Panel + Approved Inventory Panel**

- Add revision request button/modal in ReviewConsole for APPROVED_FOR_INTERNAL_USE items
- Add approved inventory listing panel
- Update ACTION_LABELS in ReviewConsole.tsx for REQUEST_INTERNAL_REVISION
- Frontend tests
- No new backend endpoints
