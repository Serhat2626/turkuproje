# FP-012 Task 07 — Admin Review Safe API Contract

**Document Status:** Implemented in Task 07  
**Feature:** FP-012 — Admin Review Console & Human Approval Workflow Hardening  
**Date:** 2026-07-06

---

## 1. Scope

Internal admin review API for human/admin review workflow actions.  
This API is NOT for public/external consumption.  
No publish/export, no public/signed URL, no CDN delivery.

## 2. Implemented Internal Admin Review Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/admin/reviews/submit | Submit content for review |
| POST | /api/admin/reviews/start | Start review (claim for review) |
| POST | /api/admin/reviews/request-changes | Request changes during review |
| POST | /api/admin/reviews/approve-internal | Approve for internal use only — NOT publish/export |
| POST | /api/admin/reviews/reject | Reject content |
| POST | /api/admin/reviews/return-to-draft | Return to draft for rework |
| POST | /api/admin/reviews/note | Add review note without state change |
| GET | /api/admin/reviews/{targetType}/{targetId}/state | Get current review state |
| GET | /api/admin/reviews/{targetType}/{targetId}/audit | Get review audit timeline |

## 3. Request Validation

- `targetType` — required (ReviewTargetType enum)
- `targetId` — required (UUID)
- `actorRole` — required (ReviewActorRole enum)
- `actorReference` — required, non-blank
- `actionType` — explicit ReviewActionType, not a generic status string
- `note` — required for add-note action
- `reasonCode` — optional, sanitized via ReviewTextSafety

No generic status update is accepted.  
No arbitrary status string is accepted.

## 4. Safe Response Model

Response: `ApiResponse<AdminReviewActionResponse>`

Fields: targetType, targetId, previousState, currentState, actionType, actorRole, auditEventType, changed, safeMessage, safeReasonCode, occurredAt

**Explicitly excluded fields:**  
providerUrl, storageKey, bucketName, bucketPath, rawPayload, base64, secret, apiKey, config, publicUrl, signedUrl, publishUrl, exportUrl, stackTrace

## 5. Safe Error Model

Uses `ApiResponse<Void>` with safe message.

| Error | HTTP Status | Message Safety |
|-------|-------------|----------------|
| ReviewApplicationException | 404 NOT_FOUND | Safe target info only |
| ReviewTransitionViolationException | 409 CONFLICT | Safe transition error |
| IllegalArgumentException | 400 BAD_REQUEST | Safe validation message |

No stack trace. No secrets. No raw payloads. No provider URLs.

## 6. Human Approval Guardrails

- CREATOR cannot approve — rejected by domain service
- SYSTEM cannot approve — rejected by domain service
- AI_GENERATED does not imply approval
- APPROVED_FOR_INTERNAL_USE is NOT publish/export approval
- No automatic approval path
- Approval returns no public/signed URL

## 7. Forbidden Endpoints and Non-Goals

This API does NOT provide:

- Generic status update endpoint
- Publish/export endpoint
- Public URL endpoint
- Signed URL endpoint
- CDN/public delivery endpoint
- Provider/storage inspection endpoint
- Admin/user readiness endpoint
- Public config inspection endpoint

## 8. Security/Privacy Boundaries

- No provider URL exposure
- No storage key exposure
- No bucket/path sensitive exposure
- No raw provider/storage payload exposure
- No base64 in API response
- No secret/API key/config exposure
- No stack trace exposure
- Note/reason content sanitized via ReviewTextSafety
- toString methods are safe on all DTOs

## 9. Test Coverage

| Test Group | Coverage |
|------------|----------|
| Action endpoints happy path | 7 actions verified |
| Query endpoints happy path | State + audit timeline |
| Request validation | 6 validation scenarios |
| Forbidden endpoint wording | Path scanning |
| Forbidden status/action | Enum value scanning |
| Safe response body | Field scanning on all DTOs |
| Safe error body | All 3 exception types verified |
| Human approval guardrails | CREATOR/SYSTEM rejection, internal-only |
| Boundary regression | Annotation + enum scanning |

## 10. Acceptance Criteria

- [x] Explicit action endpoints — no generic status update
- [x] Safe request validation
- [x] Safe response mapping via AdminReviewDtoMapper
- [x] Safe error handling — no stack trace, no secrets
- [x] Human approval guardrails preserved
- [x] No publish/export/CDN/public URL/signed URL
- [x] Comprehensive controller + safety regression tests
- [x] All tests passing
