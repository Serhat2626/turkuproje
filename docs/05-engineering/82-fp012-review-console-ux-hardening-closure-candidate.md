# FP-012 Task 11 — Review Console UX Hardening, Action Availability Model & Closure Candidate

**Document Status:** FP-012 CLOSURE_CANDIDATE (NOT CLOSED)  
**Feature:** FP-012 — Admin Review Console & Human Approval Workflow Hardening  
**Date:** 2026-07-06

---

## 1. Scope

Action availability model, availability-driven frontend console, UX hardening, and final regression sweep.
This task marks FP-012 as CLOSURE_CANDIDATE only. Final CLOSED status will be a separate task.

## 2. Implementation Summary

### Backend

* `AdminReviewAvailableAction` — safe single action availability DTO
* `AdminReviewActionAvailability` — safe container DTO
* `ReviewApplicationService.getAvailableActions()` — uses `ReviewTransitionPolicy` as source of truth
* `GET /api/admin/reviews/{targetType}/{targetId}/actions?actorRole=...` — read-only endpoint

### Frontend

* `getAvailableActions()` in `reviewApi.ts` — explicit API client method
* `AdminReviewAvailableAction` and `AdminReviewActionAvailability` types
* `ReviewConsole.tsx` rewritten with availability-driven action buttons
* UX hardening: retry button, char count, note validation, empty states, loading indicators

### Total Endpoint Inventory: 12 (7 POST + 5 GET)

| Method | Path | Purpose |
|--------|------|---------|
| POST | /submit | Submit for review |
| POST | /start | Start review |
| POST | /request-changes | Request changes |
| POST | /approve-internal | Approve for internal use |
| POST | /reject | Reject |
| POST | /return-to-draft | Return to draft |
| POST | /note | Add review note |
| GET | /worklist | List worklist items |
| GET | /{targetType}/{targetId}/context | Get target context |
| GET | /{targetType}/{targetId}/state | Get state summary |
| GET | /{targetType}/{targetId}/audit | Get audit timeline |
| GET | /{targetType}/{targetId}/actions | Get available actions |

## 3. Backend Action Availability Model

### AdminReviewAvailableAction

| Field | Type | Description |
|-------|------|-------------|
| actionType | ReviewActionType | The action |
| enabled | boolean | Whether action is allowed |
| disabledReasonCode | String | Safe reason code |
| safeLabel | String | Türkçe label |
| safeDescription | String | Türkçe description |

### AdminReviewActionAvailability

| Field | Type | Description |
|-------|------|-------------|
| targetType | ReviewTargetType | Target type |
| targetId | UUID | Target ID |
| currentState | ReviewState | Current state |
| actorRole | ReviewActorRole | Actor role |
| availableActions | List | All 7 actions with enable/disable |
| safeMessage | String | Safe message |

### Availability Logic

Source of truth: `ReviewTransitionPolicy`

* For each `ReviewActionType`, check if `targetFor(currentState, action)` returns a valid target.
* If target exists, check `canTransition(from, target, action, actorRole)`.
* CREATOR/SYSTEM: `APPROVE_FOR_INTERNAL_USE` always disabled.
* Terminal states (APPROVED_FOR_INTERNAL_USE, REJECTED): all state-changing actions disabled. Only ADD_REVIEW_NOTE enabled.
* ADD_REVIEW_NOTE: enabled for all actor roles that `canAddReviewNote()`.

## 4. Availability Endpoint

```
GET /api/admin/reviews/{targetType}/{targetId}/actions?actorRole=REVIEWER
```

Response:
```json
{
  "success": true,
  "data": {
    "targetType": "CONTENT_PROJECT",
    "targetId": "uuid",
    "currentState": "IN_REVIEW",
    "actorRole": "REVIEWER",
    "availableActions": [
      { "actionType": "APPROVE_FOR_INTERNAL_USE", "enabled": true, "safeLabel": "Dahili Onayla" },
      { "actionType": "REJECT", "enabled": true, "safeLabel": "Reddet" },
      ...
    ]
  }
}
```

## 5. Frontend Action Availability Integration

* `loadAll()` fetches state + audit + availability in parallel.
* Action buttons disabled based on `action.enabled` from backend response.
* Disabled buttons show tooltip with `safeDescription` or `disabledReasonCode`.
* CREATOR/SYSTEM: `APPROVE_FOR_INTERNAL_USE` hidden (client-side + backend enforcement).
* Empty actions state: "Kullanılabilir işlem yok" message.

## 6. UX Hardening

| Feature | Implementation |
|---------|---------------|
| Retry button | Error state shows "Tekrar Dene" button |
| Character count | Note input shows `{current}/{max}` counter |
| Note validation | REQUEST_CHANGES/REJECT require note |
| Max length guard | Note: 2000 chars, Reason: 100 chars, sliced before send |
| Empty worklist | "Sonuç bulunamadı" message |
| Empty timeline | "Henüz geçmiş yok" message |
| Empty actions | "Kullanılabilir işlem yok" message |
| Loading state | "Yükleniyor..." with test IDs |
| Safe error display | safeText applied, no stack trace |
| Action refresh | Successful action refreshes state + audit + availability |
| Worklist refresh | onActionComplete callback refreshes worklist |

## 7. Backend Test Coverage

| Test File | Tests | Category |
|-----------|-------|----------|
| AdminReviewAvailabilityTest | 19 | Availability endpoint, logic, DTO safety, inventory, forbidden actions, boundary |
| AdminReviewApiBoundaryRegressionTest | Updated | 12 endpoints, 5 GET, actions path |
| AdminReviewWorklistTest | Updated | 12 endpoints, 5 GET |
| AdminReviewControllerTest | Updated | Mock updated for getReviewStateSummary |

## 8. Frontend Test Coverage

| Test File | Tests | Category |
|-----------|-------|----------|
| ReviewConsole.test.ts | 37 | Safety + availability + UX hardening |
| ReviewWorklist.test.ts | 23 | Safety + integration |
| reviewApi.test.ts | 15 | 12 exports + forbidden |

## 9. Safety Baseline

All FP-012 safety constraints confirmed:

* ✅ No generic status update UI/endpoint
* ✅ No publish/export UI/endpoint
* ✅ No public/signed URL UI/endpoint
* ✅ No CDN/public delivery UI/endpoint
* ✅ No provider/storage inspection UI/endpoint
* ✅ No user-facing media library
* ✅ No video/audio
* ✅ No automatic approval
* ✅ No READY_FOR_PUBLISH / COMPLETED / PUBLISHED / EXPORTED
* ✅ No provider URL exposure/persistence/logging
* ✅ No storage key exposure/logging
* ✅ No bucket/path sensitive exposure
* ✅ No raw provider/storage payload exposure
* ✅ No base64 DB/DTO/log/API response
* ✅ No secret/API key/config exposure
* ✅ No stack trace display
* ✅ No unsafe HTML rendering (no dangerouslySetInnerHTML/innerHTML)
* ✅ No backend migration/schema change
* ✅ No orchestration integration
* ✅ No storage/provider behavior change
* ✅ Human review gate preserved
* ✅ AI_GENERATED does not imply approval
* ✅ APPROVED_FOR_INTERNAL_USE is not publish/export approval
* ✅ CREATOR cannot approve
* ✅ SYSTEM cannot approve
* ✅ actorRole request field is not authentication

## 10. Deferred Items

| Item | Status | Note |
|------|--------|------|
| Production auth integration / authenticated admin identity mapping | Deferred | actorRole is request param, not auth |
| Deeper role/permission model | Deferred | Current model is CREATOR/REVIEWER/ADMIN/SYSTEM |
| Production deployment route protection | Deferred | Internal API only |
| Publish/export workflow | Deferred | Separate future feature |
| Public/signed URL delivery | Deferred | Separate future feature |
| CDN/public media delivery | Deferred | Separate future feature |
| User-facing media library | Deferred | Separate future feature |
| Video/audio | Deferred | Separate future feature |
| Final production observability wiring | Deferred | Safe logging in place |
| Pagination (cursor-based) | Deferred | Simple limit-based only |
| Preview integration | Deferred | safePreviewAvailable always false |
| Display title from content | Deferred | Derived from type+ID only |

## 11. Residual Risks and Guardrails

| Risk | Guardrail |
|------|-----------|
| actorRole is request param, not authenticated identity | Backend enforces role policy; frontend hides but does not enforce |
| No RBAC/OAuth integration | All endpoints are /api/admin/ prefix — route protection expected in deployment |
| ReviewTextSafety is basic sanitization | Adequate for internal admin console; not a public-facing surface |
| No rate limiting | Internal-only; production deployment should add rate limiting |
| Terminal state note accumulation | Notes are additive; no deletion mechanism yet |

## 12. Closure Candidate Acceptance Criteria

- [x] Action availability model with safe DTOs
- [x] Availability endpoint (GET, read-only)
- [x] CREATOR/SYSTEM approve disabled (backend + frontend)
- [x] Terminal states: no state-changing actions
- [x] Frontend availability-driven action panel
- [x] UX hardening: retry, char count, note validation, empty states, loading
- [x] 12 endpoints (7 POST + 5 GET), all safe
- [x] No forbidden endpoints/actions/fields
- [x] All backend tests pass
- [x] All frontend tests pass
- [x] No migration
- [x] All safety constraints confirmed
- [x] FP-012 is CLOSURE_CANDIDATE, not CLOSED

## 13. Next Step

FP-012 final closure task — explicit CLOSED status, final test baseline confirmation.
